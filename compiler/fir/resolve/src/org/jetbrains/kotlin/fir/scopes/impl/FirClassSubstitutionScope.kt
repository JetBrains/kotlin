/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.scopes.impl

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.substitution.ChainedSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.resolve.substitution.chain
import org.jetbrains.kotlin.fir.resolve.substitution.substitutorByMap
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculator
import org.jetbrains.kotlin.fir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.jetbrains.kotlin.fir.scopes.FirTypeScope
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class FirClassSubstitutionScope(
    private val session: FirSession,
    private val useSiteMemberScope: FirTypeScope,
    scopeSession: ScopeSession,
    private val substitutor: ConeSubstitutor,
    private val skipPrivateMembers: Boolean,
    private val derivedClassId: ClassId? = null,
    private val makeExpect: Boolean = false
) : FirTypeScope() {

    private val fakeOverrideFunctions = mutableMapOf<FirFunctionSymbol<*>, FirFunctionSymbol<*>>()
    private val fakeOverrideConstructors = mutableMapOf<FirConstructorSymbol, FirConstructorSymbol>()
    private val fakeOverrideVariables = mutableMapOf<FirVariableSymbol<*>, FirVariableSymbol<*>>()

    constructor(
        session: FirSession, useSiteMemberScope: FirTypeScope, scopeSession: ScopeSession,
        substitution: Map<FirTypeParameterSymbol, ConeKotlinType>,
        skipPrivateMembers: Boolean, derivedClassId: ClassId?
    ) : this(session, useSiteMemberScope, scopeSession, substitutorByMap(substitution), skipPrivateMembers, derivedClassId)

    override fun processFunctionsByName(name: Name, processor: (FirFunctionSymbol<*>) -> Unit) {
        useSiteMemberScope.processFunctionsByName(name) process@{ original ->
            val function = fakeOverrideFunctions.getOrPut(original) { createFakeOverrideFunction(original) }
            processor(function)
        }

        return super.processFunctionsByName(name, processor)
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: FirFunctionSymbol<*>,
        processor: (FirFunctionSymbol<*>, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            functionSymbol, processor, FirTypeScope::processDirectOverriddenFunctionsWithBaseScope, fakeOverrideFunctions
        )

    private inline fun <reified D : FirCallableSymbol<*>> processDirectOverriddenWithBaseScope(
        callableSymbol: D,
        noinline processor: (D, FirTypeScope) -> ProcessorAction,
        processDirectOverriddenCallablesWithBaseScope: FirTypeScope.(D, ((D, FirTypeScope) -> ProcessorAction)) -> ProcessorAction,
        fakeOverridesMap: Map<out FirCallableSymbol<*>, FirCallableSymbol<*>>
    ): ProcessorAction {
        val original = (callableSymbol.overriddenSymbol as? D)?.takeIf { it in fakeOverridesMap }
            ?: return useSiteMemberScope.processDirectOverriddenCallablesWithBaseScope(callableSymbol, processor)

        if (!processor(original, useSiteMemberScope)) return ProcessorAction.STOP

        return useSiteMemberScope.processDirectOverriddenCallablesWithBaseScope(original, processor)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        return useSiteMemberScope.processPropertiesByName(name) process@{ original ->
            when (original) {
                is FirPropertySymbol -> {
                    val property = fakeOverrideVariables.getOrPut(original) { createFakeOverrideProperty(original) }
                    processor(property)
                }
                is FirFieldSymbol -> {
                    val field = fakeOverrideVariables.getOrPut(original) { createFakeOverrideField(original) }
                    processor(field)
                }
                is FirAccessorSymbol -> {
                    val accessor = fakeOverrideVariables.getOrPut(original) { createFakeOverrideAccessor(original) }
                    processor(accessor)
                }
                else -> {
                    processor(original)
                }
            }
        }
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: FirPropertySymbol,
        processor: (FirPropertySymbol, FirTypeScope) -> ProcessorAction
    ): ProcessorAction =
        processDirectOverriddenWithBaseScope(
            propertySymbol, processor, FirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
            fakeOverrideVariables
        )

    override fun processClassifiersByNameWithSubstitution(name: Name, processor: (FirClassifierSymbol<*>, ConeSubstitutor) -> Unit) {
        useSiteMemberScope.processClassifiersByNameWithSubstitution(name) { symbol, substitutor ->
            processor(symbol, substitutor.chain(this.substitutor))
        }
    }

    private val typeCalculator =
        (scopeSession.returnTypeCalculator as ReturnTypeCalculator?) ?: ReturnTypeCalculatorForFullBodyResolve()

    private fun ConeKotlinType.substitute(): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun ConeKotlinType.substitute(substitutor: ConeSubstitutor): ConeKotlinType? {
        return substitutor.substituteOrNull(this)
    }

    private fun createFakeOverrideFunction(original: FirFunctionSymbol<*>): FirFunctionSymbol<*> {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = when (original) {
            is FirNamedFunctionSymbol -> original.fir
            is FirConstructorSymbol -> return original
            else -> throw AssertionError("Should not be here")
        }
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val (newTypeParameters, newReceiverType, newReturnType, newSubstitutor) = createSubstitutedData(member)
        val newParameterTypes = member.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReceiverType == null && newReturnType == null && newParameterTypes.all { it == null } &&
            newTypeParameters === member.typeParameters) {
            return original
        }

        /*
         * Member functions can't capture type parameters, so
         *   it's safe to cast newTypeParameters to List<FirTypeParameter>
         */
        @Suppress("UNCHECKED_CAST")
        return createFakeOverrideFunction(
            session,
            member,
            original,
            newReceiverType,
            newReturnType,
            newParameterTypes,
            newTypeParameters as List<FirTypeParameter>,
            derivedClassId,
            makeExpect
        )
    }

    private fun createFakeOverrideConstructor(original: FirConstructorSymbol): FirConstructorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val constructor = original.fir

        val (newTypeParameters, _, newReturnType, newSubstitutor) = createSubstitutedData(constructor)
        val newParameterTypes = constructor.valueParameters.map {
            it.returnTypeRef.coneType.substitute(newSubstitutor)
        }

        if (newReturnType == null && newParameterTypes.all { it == null } && newTypeParameters === constructor.typeParameters) {
            return original
        }
        return createFakeOverrideConstructor(
            FirConstructorSymbol(original.callableId, overriddenSymbol = original),
            session, constructor, newReturnType, newParameterTypes, newTypeParameters, makeExpect
        ).symbol
    }

    private fun createFakeOverrideProperty(original: FirPropertySymbol): FirPropertySymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val (newTypeParameters, newReceiverType, newReturnType, _) = createSubstitutedData(member)
        if (newReceiverType == null &&
            newReturnType == null && newTypeParameters === member.typeParameters
        ) {
            return original
        }

        @Suppress("UNCHECKED_CAST")
        return createFakeOverrideProperty(
            session,
            member,
            original,
            newReceiverType,
            newReturnType,
            newTypeParameters as List<FirTypeParameter>,
            derivedClassId,
            makeExpect
        )
    }

    private data class SubstitutedData(
        val typeParameters: List<FirTypeParameterRef>,
        val receiverType: ConeKotlinType?,
        val returnType: ConeKotlinType?,
        val substitutor: ConeSubstitutor
    )

    private fun createSubstitutedData(member: FirCallableMemberDeclaration<*>): SubstitutedData {
        val (newTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
            member as FirTypeParameterRefsOwner,
            substitutor,
            forceTypeParametersRecreation = derivedClassId != null && derivedClassId != member.symbol.callableId.classId
        )

        val receiverType = member.receiverTypeRef?.coneType
        val newReceiverType = receiverType?.substitute(substitutor)

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute(substitutor)
        return SubstitutedData(newTypeParameters, newReceiverType, newReturnType, substitutor)
    }

    private fun createFakeOverrideField(original: FirFieldSymbol): FirFieldSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute() ?: return original

        return createFakeOverrideField(session, member, original, newReturnType, derivedClassId)
    }

    private fun createFakeOverrideAccessor(original: FirAccessorSymbol): FirAccessorSymbol {
        if (substitutor == ConeSubstitutor.Empty) return original
        val member = original.fir as FirSyntheticProperty
        if (skipPrivateMembers && member.visibility == Visibilities.Private) return original

        val returnType = typeCalculator.tryCalculateReturnType(member).type
        val newReturnType = returnType.substitute()

        val newParameterTypes = member.getter.valueParameters.map {
            it.returnTypeRef.coneType.substitute()
        }

        if (newReturnType == null && newParameterTypes.all { it == null }) {
            return original
        }

        return createFakeOverrideAccessor(session, member, original, newReturnType, newParameterTypes)
    }

    companion object {
        fun createFakeOverrideFunction(
            session: FirSession,
            baseFunction: FirSimpleFunction,
            baseSymbol: FirNamedFunctionSymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameter>? = null,
            derivedClassId: ClassId? = null,
            isExpect: Boolean = baseFunction.isExpect
        ): FirNamedFunctionSymbol {
            val symbol = FirNamedFunctionSymbol(
                CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseFunction.name),
                isFakeOverride = true, overriddenSymbol = baseSymbol
            )
            createFakeOverrideFunction(
                symbol, session, baseFunction, newReceiverType, newReturnType, newParameterTypes, newTypeParameters, isExpect
            )
            return symbol
        }

        private fun createFakeOverrideFunction(
            fakeOverrideSymbol: FirFunctionSymbol<FirSimpleFunction>,
            session: FirSession,
            baseFunction: FirSimpleFunction,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameter>? = null,
            isExpect: Boolean = baseFunction.isExpect,
        ): FirSimpleFunction {
            // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
            // As second alternative, we can invent some light-weight kind of FirRegularClass
            return createCopyForFirFunction(
                fakeOverrideSymbol,
                baseFunction,
                session,
                FirDeclarationOrigin.FakeOverride,
                isExpect,
                newParameterTypes,
                newTypeParameters,
                newReceiverType,
                newReturnType
            )
        }

        fun createCopyForFirFunction(
            newSymbol: FirFunctionSymbol<FirSimpleFunction>,
            baseFunction: FirSimpleFunction,
            session: FirSession,
            origin: FirDeclarationOrigin,
            isExpect: Boolean = baseFunction.isExpect,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameter>? = null,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newModality: Modality? = null,
            newVisibility: Visibility? = null,
        ): FirSimpleFunction {
            return buildSimpleFunction {
                source = baseFunction.source
                this.session = session
                this.origin = origin
                name = baseFunction.name
                status = baseFunction.status.updatedStatus(isExpect, newModality, newVisibility)
                symbol = newSymbol
                resolvePhase = baseFunction.resolvePhase

                typeParameters += configureAnnotationsTypeParametersAndSignature(
                    session, baseFunction, newParameterTypes, newTypeParameters, newReceiverType, newReturnType
                ).filterIsInstance<FirTypeParameter>()
            }
        }

        private fun createFakeOverrideConstructor(
            fakeOverrideSymbol: FirConstructorSymbol,
            session: FirSession,
            baseConstructor: FirConstructor,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null,
            newTypeParameters: List<FirTypeParameterRef>? = null,
            isExpect: Boolean = baseConstructor.isExpect
        ): FirConstructor {
            // TODO: consider using here some light-weight functions instead of pseudo-real FirMemberFunctionImpl
            // As second alternative, we can invent some light-weight kind of FirRegularClass
            return buildConstructor {
                source = baseConstructor.source
                this.session = session
                origin = FirDeclarationOrigin.FakeOverride
                receiverTypeRef = baseConstructor.receiverTypeRef?.withReplacedConeType(null)
                status = baseConstructor.status.updatedStatus(isExpect)
                symbol = fakeOverrideSymbol
                resolvePhase = baseConstructor.resolvePhase

                typeParameters += configureAnnotationsTypeParametersAndSignature(
                    session, baseConstructor, newParameterTypes, newTypeParameters, null, newReturnType
                )
            }
        }

        private fun FirFunctionBuilder.configureAnnotationsTypeParametersAndSignature(
            session: FirSession,
            baseFunction: FirFunction<*>,
            newParameterTypes: List<ConeKotlinType?>?,
            newTypeParameters: List<FirTypeParameterRef>?,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null
        ): List<FirTypeParameterRef> {
            return when {
                baseFunction.typeParameters.isEmpty() -> {
                    configureAnnotationsAndSignature(session, baseFunction, newParameterTypes, newReceiverType, newReturnType)
                    emptyList()
                }
                newTypeParameters == null -> {
                    val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                        baseFunction, ConeSubstitutor.Empty
                    )
                    val copiedParameterTypes = baseFunction.valueParameters.map {
                        substitutor.substituteOrNull(it.returnTypeRef.coneType)
                    }
                    val (copiedReceiverType, copiedReturnType) = substituteReceiverAndReturnType(
                        baseFunction as FirCallableMemberDeclaration<*>, newReceiverType, newReturnType, substitutor
                    )
                    configureAnnotationsAndSignature(session, baseFunction, copiedParameterTypes, copiedReceiverType, copiedReturnType)
                    copiedTypeParameters
                }
                else -> {
                    configureAnnotationsAndSignature(session, baseFunction, newParameterTypes, newReceiverType, newReturnType)
                    newTypeParameters
                }
            }
        }

        private fun FirFunctionBuilder.configureAnnotationsAndSignature(
            session: FirSession,
            baseFunction: FirFunction<*>,
            newParameterTypes: List<ConeKotlinType?>?,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null
        ) {
            annotations += baseFunction.annotations
            returnTypeRef = baseFunction.returnTypeRef.withReplacedConeType(newReturnType)
            if (this is FirSimpleFunctionBuilder) {
                receiverTypeRef = baseFunction.receiverTypeRef?.withReplacedConeType(newReceiverType)
            }
            valueParameters += baseFunction.valueParameters.zip(
                newParameterTypes ?: List(baseFunction.valueParameters.size) { null }
            ) { valueParameter, newType ->
                buildValueParameterCopy(valueParameter) {
                    origin = FirDeclarationOrigin.FakeOverride
                    returnTypeRef = valueParameter.returnTypeRef.withReplacedConeType(newType)
                    symbol = FirVariableSymbol(valueParameter.symbol.callableId)
                }
            }
        }

        fun createFakeOverrideProperty(
            session: FirSession,
            baseProperty: FirProperty,
            baseSymbol: FirPropertySymbol,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newTypeParameters: List<FirTypeParameter>? = null,
            derivedClassId: ClassId? = null,
            isExpect: Boolean = baseProperty.isExpect
        ): FirPropertySymbol {
            val symbol = FirPropertySymbol(
                CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseProperty.name),
                isFakeOverride = true, overriddenSymbol = baseSymbol
            )
            createCopyForFirProperty(
                symbol, baseProperty, session, isExpect,
                newTypeParameters, newReceiverType, newReturnType
            )
            return symbol
        }

        fun createCopyForFirProperty(
            newSymbol: FirPropertySymbol,
            baseProperty: FirProperty,
            session: FirSession,
            isExpect: Boolean = baseProperty.isExpect,
            newTypeParameters: List<FirTypeParameter>? = null,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null,
            newModality: Modality? = null,
            newVisibility: Visibility? = null,
        ): FirProperty {
            return buildProperty {
                source = baseProperty.source
                this.session = session
                origin = FirDeclarationOrigin.FakeOverride
                name = baseProperty.name
                isVar = baseProperty.isVar
                this.symbol = newSymbol
                isLocal = false
                status = baseProperty.status.updatedStatus(isExpect, newModality, newVisibility)

                resolvePhase = baseProperty.resolvePhase
                typeParameters += configureAnnotationsTypeParametersAndSignature(
                    baseProperty,
                    newTypeParameters,
                    newReceiverType,
                    newReturnType
                )
            }
        }

        private fun FirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
            baseProperty: FirProperty,
            newTypeParameters: List<FirTypeParameter>?,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null
        ): List<FirTypeParameter> {
            return when {
                baseProperty.typeParameters.isEmpty() -> {
                    configureAnnotationsAndSignature(baseProperty, newReceiverType, newReturnType)
                    emptyList()
                }
                newTypeParameters == null -> {
                    val (copiedTypeParameters, substitutor) = createNewTypeParametersAndSubstitutor(
                        baseProperty, ConeSubstitutor.Empty
                    )
                    val (copiedReceiverType, copiedReturnType) = substituteReceiverAndReturnType(
                        baseProperty, newReceiverType, newReturnType, substitutor
                    )
                    configureAnnotationsAndSignature(baseProperty, copiedReceiverType, copiedReturnType)
                    copiedTypeParameters.filterIsInstance<FirTypeParameter>()
                }
                else -> {
                    configureAnnotationsAndSignature(baseProperty, newReceiverType, newReturnType)
                    newTypeParameters
                }
            }
        }

        private fun substituteReceiverAndReturnType(
            baseCallable: FirCallableMemberDeclaration<*>,
            newReceiverType: ConeKotlinType?,
            newReturnType: ConeKotlinType?,
            substitutor: ConeSubstitutor
        ): Pair<ConeKotlinType?, ConeKotlinType?> {
            val copiedReceiverType = newReceiverType?.let {
                substitutor.substituteOrNull(it)
            } ?: baseCallable.receiverTypeRef?.let {
                substitutor.substituteOrNull(it.coneType)
            }
            val copiedReturnType = newReturnType?.let {
                substitutor.substituteOrNull(it)
            } ?: baseCallable.returnTypeRef.let {
                substitutor.substituteOrNull(it.coneType)
            }
            return copiedReceiverType to copiedReturnType
        }

        private fun FirPropertyBuilder.configureAnnotationsAndSignature(
            baseProperty: FirProperty,
            newReceiverType: ConeKotlinType? = null,
            newReturnType: ConeKotlinType? = null
        ) {
            annotations += baseProperty.annotations
            returnTypeRef = baseProperty.returnTypeRef.withReplacedConeType(newReturnType)
            receiverTypeRef = baseProperty.receiverTypeRef?.withReplacedConeType(newReceiverType)
        }

        fun createFakeOverrideField(
            session: FirSession,
            baseField: FirField,
            baseSymbol: FirFieldSymbol,
            newReturnType: ConeKotlinType? = null,
            derivedClassId: ClassId? = null
        ): FirFieldSymbol {
            val symbol = FirFieldSymbol(
                CallableId(derivedClassId ?: baseSymbol.callableId.classId!!, baseField.name)
            )
            buildField {
                source = baseField.source
                this.session = session
                origin = FirDeclarationOrigin.FakeOverride
                resolvePhase = baseField.resolvePhase
                returnTypeRef = baseField.returnTypeRef.withReplacedConeType(newReturnType)
                name = baseField.name
                this.symbol = symbol
                isVar = baseField.isVar
                status = baseField.status
                resolvePhase = baseField.resolvePhase
                annotations += baseField.annotations
            }
            return symbol
        }

        fun createFakeOverrideAccessor(
            session: FirSession,
            baseProperty: FirSyntheticProperty,
            baseSymbol: FirAccessorSymbol,
            newReturnType: ConeKotlinType? = null,
            newParameterTypes: List<ConeKotlinType?>? = null
        ): FirAccessorSymbol {
            val functionSymbol = FirNamedFunctionSymbol(baseSymbol.accessorId)
            val function = createFakeOverrideFunction(
                functionSymbol, session, baseProperty.getter.delegate, null, newReturnType, newParameterTypes
            )
            return buildSyntheticProperty {
                this.session = session
                name = baseProperty.name
                symbol = FirAccessorSymbol(baseSymbol.callableId, baseSymbol.accessorId)
                delegateGetter = function
            }.symbol
        }

        private fun FirDeclarationStatus.updatedStatus(
            isExpect: Boolean,
            newModality: Modality? = null,
            newVisibility: Visibility? = null,
        ): FirDeclarationStatus {
            return if (this.isExpect == isExpect && newModality == null && newVisibility == null) {
                this
            } else {
                require(this is FirDeclarationStatusImpl) { "Unexpected class ${this::class}" }
                this.resolved(newVisibility ?: visibility, newModality ?: modality!!).apply {
                    this.isExpect = isExpect
                }
            }
        }

        // Returns a list of type parameters, and a substitutor that should be used for all other types
        private fun createNewTypeParametersAndSubstitutor(
            member: FirTypeParameterRefsOwner,
            substitutor: ConeSubstitutor,
            forceTypeParametersRecreation: Boolean = true
        ): Pair<List<FirTypeParameterRef>, ConeSubstitutor> {
            if (member.typeParameters.isEmpty()) return Pair(member.typeParameters, substitutor)
            val newTypeParameters = member.typeParameters.map { typeParameter ->
                if (typeParameter !is FirTypeParameter) return@map null
                FirTypeParameterBuilder().apply {
                    source = typeParameter.source
                    session = typeParameter.session
                    origin = FirDeclarationOrigin.FakeOverride
                    name = typeParameter.name
                    symbol = FirTypeParameterSymbol()
                    variance = typeParameter.variance
                    isReified = typeParameter.isReified
                    annotations += typeParameter.annotations
                }
            }

            val substitutionMapForNewParameters = member.typeParameters.zip(newTypeParameters).mapNotNull { (original, new) ->
                if (new != null)
                    Pair(original.symbol, ConeTypeParameterTypeImpl(new.symbol.toLookupTag(), isNullable = false))
                else null
            }.toMap()

            val additionalSubstitutor = substitutorByMap(substitutionMapForNewParameters)

            var wereChangesInTypeParameters = forceTypeParametersRecreation
            for ((newTypeParameter, oldTypeParameter) in newTypeParameters.zip(member.typeParameters)) {
                if (newTypeParameter == null) continue
                val original = oldTypeParameter as FirTypeParameter
                for (boundTypeRef in original.bounds) {
                    val typeForBound = boundTypeRef.coneType
                    val substitutedBound = substitutor.substituteOrNull(typeForBound)
                    if (substitutedBound != null) {
                        wereChangesInTypeParameters = true
                    }
                    newTypeParameter.bounds +=
                        buildResolvedTypeRef {
                            source = boundTypeRef.source
                            type = additionalSubstitutor.substituteOrSelf(substitutedBound ?: typeForBound)
                        }
                }
            }

            if (!wereChangesInTypeParameters) return Pair(member.typeParameters, substitutor)
            return Pair(
                newTypeParameters.mapIndexed { index, builder -> builder?.build() ?: member.typeParameters[index] },
                ChainedSubstitutor(substitutor, additionalSubstitutor)
            )
        }
    }

    override fun processDeclaredConstructors(processor: (FirConstructorSymbol) -> Unit) {
        useSiteMemberScope.processDeclaredConstructors process@{ original ->
            val constructor = fakeOverrideConstructors.getOrPut(original) { createFakeOverrideConstructor(original) }
            processor(constructor)
        }
    }

    override fun getCallableNames(): Set<Name> {
        return useSiteMemberScope.getCallableNames()
    }

    override fun getClassifierNames(): Set<Name> {
        return useSiteMemberScope.getClassifierNames()
    }
}
