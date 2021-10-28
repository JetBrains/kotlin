/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isOperator
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethodCopy
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameterCopy
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmSignature
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.load.java.BuiltinSpecialProperties
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.ERASED_COLLECTION_PARAMETER_NAMES
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.sameAsRenamedInJvmBuiltin
import org.jetbrains.kotlin.load.java.getPropertyNamesCandidatesByAccessorName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker

class JavaClassUseSiteMemberScope(
    klass: FirJavaClass,
    session: FirSession,
    superTypesScope: FirTypeScope,
    declaredMemberScope: FirContainingNamesAwareScope
) : AbstractFirUseSiteMemberScope(
    session,
    JavaOverrideChecker(session, klass.javaTypeParameterStack, superTypesScope, considerReturnTypeKinds = true),
    superTypesScope,
    declaredMemberScope
) {
    private val typeParameterStack = klass.javaTypeParameterStack
    private val specialFunctions = hashMapOf<Name, Collection<FirNamedFunctionSymbol>>()
    private val accessorByNameMap = hashMapOf<Name, FirSyntheticPropertySymbol>()
    
    private val canUseSpecialGetters: Boolean by lazy { !klass.hasKotlinSuper(session) }

    private val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        declaredMemberScope.getCallableNames() + superTypesScope.getCallableNames()
    }

    override fun getCallableNames(): Set<Name> = callableNamesCached

    override fun getClassifierNames(): Set<Name> {
        return declaredMemberScope.getClassifierNames() + superTypesScope.getClassifierNames()
    }

    private fun generateAccessorSymbol(
        getterSymbol: FirNamedFunctionSymbol,
        setterSymbol: FirNamedFunctionSymbol?,
        property: FirProperty,
        takeModalityFromGetter: Boolean,
    ): FirSyntheticPropertySymbol {
        return accessorByNameMap.getOrPut(property.name) {
            buildSyntheticProperty {
                moduleData = session.moduleData
                name = property.name
                symbol = FirJavaOverriddenSyntheticPropertySymbol(
                    getterId = getterSymbol.callableId,
                    propertyId = CallableId(getterSymbol.callableId.packageName, getterSymbol.callableId.className, property.name)
                )
                delegateGetter = getterSymbol.fir
                delegateSetter = setterSymbol?.fir
                status = getterSymbol.fir.status.copy(
                    newModality = if (takeModalityFromGetter) {
                        delegateGetter.modality ?: property.modality
                    } else {
                        chooseModalityForAccessor(property, delegateGetter)
                    }
                )
                deprecation = getDeprecationsFromAccessors(delegateGetter, delegateSetter, session.languageVersionSettings.apiVersion)
            }.symbol
        }
    }

    private fun chooseModalityForAccessor(property: FirProperty, getter: FirSimpleFunction): Modality? {
        val a = property.modality
        val b = getter.modality

        if (a == null) return b
        if (b == null) return a

        return minOf(a, b)
    }

    override fun processPropertiesByName(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        val fields = mutableSetOf<FirCallableSymbol<*>>()
        val fieldNames = mutableSetOf<Name>()

        // fields
        declaredMemberScope.processPropertiesByName(name) processor@{ variableSymbol ->
            if (variableSymbol.isStatic) return@processor
            fields += variableSymbol
            fieldNames += variableSymbol.fir.name
            processor(variableSymbol)
        }

        val fromSupertypes = superTypesScope.getProperties(name)

        for (propertyFromSupertype in fromSupertypes) {
            if (propertyFromSupertype is FirFieldSymbol) {
                if (propertyFromSupertype.fir.name !in fieldNames) {
                    processor(propertyFromSupertype)
                }
                continue
            }
            if (propertyFromSupertype !is FirPropertySymbol) continue
            val overrideInClass =
                propertyFromSupertype.createOverridePropertyIfExists(declaredMemberScope, takeModalityFromGetter = true)
                    ?: propertyFromSupertype.createOverridePropertyIfExists(superTypesScope, takeModalityFromGetter = false)
            when {
                overrideInClass != null -> {
                    directOverriddenProperties.getOrPut(overrideInClass) { mutableListOf() }.add(propertyFromSupertype)
                    overrideByBase[propertyFromSupertype] = overrideInClass
                    processor(overrideInClass)
                }
                else -> processor(propertyFromSupertype)
            }
        }
    }

    private fun FirVariableSymbol<*>.createOverridePropertyIfExists(
        scope: FirScope,
        takeModalityFromGetter: Boolean
    ): FirPropertySymbol? {
        if (this !is FirPropertySymbol) return null
        val getterSymbol = this.findGetterOverride(scope) ?: return null
        val setterSymbol =
            if (this.fir.isVar)
                this.findSetterOverride(scope) ?: return null
            else
                null
        if (setterSymbol != null && setterSymbol.fir.modality != getterSymbol.fir.modality) return null

        return generateAccessorSymbol(getterSymbol, setterSymbol, fir, takeModalityFromGetter)
    }

    private fun FirPropertySymbol.findGetterOverride(
        scope: FirScope,
    ): FirNamedFunctionSymbol? {
        val specialGetterName = if (canUseSpecialGetters) getBuiltinSpecialPropertyGetterName() else null
        if (specialGetterName != null) {
            return findGetterByName(specialGetterName.asString(), scope)
        }

        return findGetterByName(JvmAbi.getterName(fir.name.asString()), scope)
    }

    private fun FirPropertySymbol.findGetterByName(
        getterName: String,
        scope: FirScope,
    ): FirNamedFunctionSymbol? {
        val propertyFromSupertype = fir
        val expectedReturnType = propertyFromSupertype.returnTypeRef.coneTypeSafe<ConeKotlinType>()
        return scope.getFunctions(Name.identifier(getterName)).firstNotNullOfOrNull factory@{ candidateSymbol ->
            val candidate = candidateSymbol.fir
            if (candidate.valueParameters.isNotEmpty()) return@factory null

            val candidateReturnType = candidate.returnTypeRef.toConeKotlinTypeProbablyFlexible(session, typeParameterStack)

            candidateSymbol.takeIf {
                // TODO: Decide something for the case when property type is not computed yet
                expectedReturnType == null || AbstractTypeChecker.isSubtypeOf(session.typeContext, candidateReturnType, expectedReturnType)
            }
        }
    }

    private fun FirPropertySymbol.findSetterOverride(
        scope: FirScope,
    ): FirNamedFunctionSymbol? {
        val propertyType = fir.returnTypeRef.coneTypeSafe<ConeKotlinType>() ?: return null
        return scope.getFunctions(Name.identifier(JvmAbi.setterName(fir.name.asString()))).firstNotNullOfOrNull factory@{ candidateSymbol ->
            val candidate = candidateSymbol.fir
            if (candidate.valueParameters.size != 1) return@factory null

            if (!candidate.returnTypeRef.toConeKotlinTypeProbablyFlexible(session, typeParameterStack).isUnit) return@factory null

            val parameterType =
                candidate.valueParameters.single().returnTypeRef.toConeKotlinTypeProbablyFlexible(session, typeParameterStack)

            candidateSymbol.takeIf {
                AbstractTypeChecker.equalTypes(session.typeContext, parameterType, propertyType)
            }
        }
    }

    private fun FirPropertySymbol.getBuiltinSpecialPropertyGetterName(): Name? {
        var result: Name? = null

        superTypesScope.processOverriddenPropertiesAndSelf(this) { overridden ->
            val fqName = overridden.fir.containingClass()?.classId?.asSingleFqName()?.child(overridden.fir.name)

            BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[fqName]?.let { name ->
                result = name
                return@processOverriddenPropertiesAndSelf ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }

    override fun processFunctionsByName(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        val potentialPropertyNames = getPropertyNamesCandidatesByAccessorName(name)

        val renamedSpecialBuiltInNames = SpecialGenericSignatures.getBuiltinFunctionNamesByJvmName(name)

        if (potentialPropertyNames.isEmpty() && renamedSpecialBuiltInNames.isEmpty() &&
            !name.sameAsBuiltinMethodWithErasedValueParameters && !name.sameAsRenamedInJvmBuiltin
        ) {
            return super.processFunctionsByName(name, processor)
        }

        val overriddenProperties = potentialPropertyNames.flatMap(this::getProperties).filterIsInstance<FirPropertySymbol>()

        specialFunctions.getOrPut(name) {
            doProcessSpecialFunctions(name, overriddenProperties, renamedSpecialBuiltInNames)
        }.forEach {
            processor(it)
        }
    }

    private fun doProcessSpecialFunctions(
        name: Name,
        overriddenProperties: List<FirPropertySymbol>,
        renamedSpecialBuiltInNames: List<Name>
    ): List<FirNamedFunctionSymbol> {
        val result = mutableListOf<FirNamedFunctionSymbol>()

        declaredMemberScope.processFunctionsByName(name) { functionSymbol ->
            if (functionSymbol.isStatic) return@processFunctionsByName
            if (overriddenProperties.none { it.isOverriddenInClassBy(functionSymbol) } &&
                !functionSymbol.doesOverrideRenamedBuiltins(renamedSpecialBuiltInNames) &&
                !functionSymbol.shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters()
            ) {
                result += functionSymbol
            }
        }

        addOverriddenSpecialMethods(name, result, declaredMemberScope)

        val overrideCandidates = result.toMutableSet()

        superTypesScope.processFunctionsByName(name) { functionSymbol ->
            val overriddenBy = functionSymbol.getOverridden(overrideCandidates)
            if (overriddenBy == null && overriddenProperties.none { it.isOverriddenInClassBy(functionSymbol) }) {
                result += functionSymbol
            }
        }

        return result
    }

    private fun FirPropertySymbol.isOverriddenInClassBy(functionSymbol: FirNamedFunctionSymbol): Boolean {
        val fir = fir as? FirSyntheticProperty ?: return false

        if (fir.getter.delegate.symbol == functionSymbol || fir.setter?.delegate?.symbol == functionSymbol) return true

        val currentJvmDescriptor = functionSymbol.fir.computeJvmDescriptor(includeReturnType = false)
        val getterJvmDescriptor = fir.getter.delegate.computeJvmDescriptor(includeReturnType = false)
        val setterJvmDescriptor = fir.setter?.delegate?.computeJvmDescriptor(includeReturnType = false)

        return currentJvmDescriptor == getterJvmDescriptor || currentJvmDescriptor == setterJvmDescriptor
    }

    private fun addOverriddenSpecialMethods(
        name: Name,
        result: MutableList<FirNamedFunctionSymbol>,
        scope: FirScope,
    ) {
        superTypesScope.processFunctionsByName(name) { fromSupertype ->
            obtainOverrideForBuiltinWithDifferentJvmName(fromSupertype, scope, name)?.let {
                directOverriddenFunctions[it] = listOf(fromSupertype)
                overrideByBase[fromSupertype] = it
                result += it
            }

            obtainOverrideForBuiltInWithErasedValueParametersInJava(fromSupertype, scope)?.let {
                directOverriddenFunctions[it] = listOf(fromSupertype)
                overrideByBase[fromSupertype] = it
                result += it
            }
        }
    }

    private fun obtainOverrideForBuiltinWithDifferentJvmName(
        symbol: FirNamedFunctionSymbol,
        scope: FirScope,
        name: Name,
    ): FirNamedFunctionSymbol? {
        val overriddenBuiltin = symbol.getOverriddenBuiltinWithDifferentJvmName() ?: return null

        //if (unrelated) method with special name is already defined, we don't add renamed method at all
        //otherwise  we get methods ambiguity
        val alreadyDefined = declaredMemberScope.getFunctions(name).any { declaredSymbol ->
            overrideChecker.isOverriddenFunction(declaredSymbol.fir, symbol.fir)
        }
        if (alreadyDefined) return null

        val nameInJava =
            SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME[overriddenBuiltin.fir.computeJvmSignature() ?: return null]
                ?: return null

        for (candidateSymbol in scope.getFunctions(nameInJava)) {
            val candidateFir = candidateSymbol.fir
            val renamedCopy = buildJavaMethodCopy(candidateFir) {
                this.name = name
                this.symbol = FirNamedFunctionSymbol(CallableId(candidateFir.symbol.callableId.classId!!, name))
                this.status = candidateFir.status.copy(isOperator = symbol.isOperator)
            }.apply {
                initialSignatureAttr = candidateFir
            }

            if (overrideChecker.isOverriddenFunction(renamedCopy, overriddenBuiltin.fir)) {
                return renamedCopy.symbol
            }
        }

        return null
    }

    private fun obtainOverrideForBuiltInWithErasedValueParametersInJava(
        symbol: FirNamedFunctionSymbol,
        scope: FirScope,
    ): FirNamedFunctionSymbol? {
        val overriddenBuiltin =
            symbol.getOverriddenBuiltinFunctionWithErasedValueParametersInJava()
                ?: return null

        return createOverrideForBuiltinFunctionWithErasedParameterIfNeeded(symbol, overriddenBuiltin, scope)
    }

    private fun createOverrideForBuiltinFunctionWithErasedParameterIfNeeded(
        fromSupertype: FirNamedFunctionSymbol,
        overriddenBuiltin: FirNamedFunctionSymbol,
        scope: FirScope,
    ): FirNamedFunctionSymbol? {
        return scope.getFunctions(overriddenBuiltin.fir.name).firstOrNull { candidateOverride ->
            candidateOverride.fir.computeJvmDescriptor() == overriddenBuiltin.fir.computeJvmDescriptor() &&
                    candidateOverride.hasErasedParameters()
        }?.let { override ->
            buildJavaMethodCopy(override.fir) {
                this.valueParameters.clear()
                override.fir.valueParameters.zip(fromSupertype.fir.valueParameters)
                    .mapTo(this.valueParameters) { (overrideParameter, parameterFromSupertype) ->
                        buildJavaValueParameterCopy(overrideParameter) {
                            this@buildJavaValueParameterCopy.returnTypeRef = parameterFromSupertype.returnTypeRef
                        }
                    }

                symbol = FirNamedFunctionSymbol(override.callableId)
            }.apply {
                initialSignatureAttr = override.fir
            }.symbol
        }
    }

    // It's either overrides Collection.contains(Object) or Collection.containsAll(Collection<?>) or similar methods
    private fun FirNamedFunctionSymbol.hasErasedParameters(): Boolean {
        val valueParameter = fir.valueParameters.first()
        val parameterType = valueParameter.returnTypeRef.toConeKotlinTypeProbablyFlexible(session, typeParameterStack)
        val upperBound = parameterType.upperBoundIfFlexible()
        if (upperBound !is ConeClassLikeType) return false

        if (fir.name.asString() in ERASED_COLLECTION_PARAMETER_NAMES) {
            require(upperBound.lookupTag.classId == StandardClassIds.Collection) {
                "Unexpected type: ${upperBound.lookupTag.classId}"
            }

            return upperBound.typeArguments.singleOrNull() is ConeStarProjection
        }

        return upperBound.classId == StandardClassIds.Any
    }

    private fun FirNamedFunctionSymbol.doesOverrideRenamedBuiltins(renamedSpecialBuiltInNames: List<Name>): Boolean {
        return renamedSpecialBuiltInNames.any {
            // e.g. 'removeAt' or 'toInt'
                builtinName ->
            val builtinSpecialFromSuperTypes =
                getFunctionsFromSupertypes(builtinName).filter { it.getOverriddenBuiltinWithDifferentJvmName() != null }
            if (builtinSpecialFromSuperTypes.isEmpty()) return@any false

            val currentJvmDescriptor = fir.computeJvmDescriptor(customName = builtinName.asString())

            builtinSpecialFromSuperTypes.any { builtinSpecial ->
                builtinSpecial.fir.computeJvmDescriptor() == currentJvmDescriptor
            }
        }
    }

    private fun FirFunction.computeJvmSignature(): String? {
        return computeJvmSignature { it.toConeKotlinTypeProbablyFlexible(session, typeParameterStack) }
    }

    private fun FirFunction.computeJvmDescriptor(customName: String? = null, includeReturnType: Boolean = false): String {
        return computeJvmDescriptor(customName, includeReturnType) {
            it.toConeKotlinTypeProbablyFlexible(
                session,
                typeParameterStack
            )
        }
    }

    private fun getFunctionsFromSupertypes(name: Name): List<FirNamedFunctionSymbol> {
        val result = mutableListOf<FirNamedFunctionSymbol>()
        superTypesScope.processFunctionsByName(name) {
            result += it
        }

        return result
    }

    private fun FirNamedFunctionSymbol.getOverriddenBuiltinWithDifferentJvmName(): FirNamedFunctionSymbol? {
        var result: FirNamedFunctionSymbol? = null

        if (SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME.containsKey(this.fir.computeJvmSignature())) {
            return this
        }

        superTypesScope.processOverriddenFunctions(this) {
            if (!it.isFromBuiltInClass(session)) return@processOverriddenFunctions ProcessorAction.NEXT
            if (SpecialGenericSignatures.SIGNATURE_TO_JVM_REPRESENTATION_NAME.containsKey(it.fir.computeJvmSignature())) {
                result = it
                return@processOverriddenFunctions ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }

    private fun FirNamedFunctionSymbol.shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters(): Boolean {
        val name = fir.name
        if (!name.sameAsBuiltinMethodWithErasedValueParameters) return false
        val candidatesToOverride =
            getFunctionsFromSupertypes(name).mapNotNull {
                it.getOverriddenBuiltinFunctionWithErasedValueParametersInJava()
            }

        val jvmDescriptor = fir.computeJvmDescriptor()

        return candidatesToOverride.any { candidate ->
            candidate.fir.computeJvmDescriptor() == jvmDescriptor && this.hasErasedParameters()
        }
    }

    private fun FirNamedFunctionSymbol.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(): FirNamedFunctionSymbol? {
        var result: FirNamedFunctionSymbol? = null

        superTypesScope.processOverriddenFunctionsAndSelf(this) {
            if (it.fir.computeJvmSignature() in SpecialGenericSignatures.ERASED_VALUE_PARAMETERS_SIGNATURES) {
                result = it
                return@processOverriddenFunctionsAndSelf ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }

    /**
     * Checks if class has any kotlin super-types apart from builtins and interfaces
     */
    private fun FirRegularClass.hasKotlinSuper(session: FirSession, visited: MutableSet<FirRegularClass> = mutableSetOf()): Boolean =
        when {
            !visited.add(this) -> false
            this is FirJavaClass -> superConeTypes.any { type ->
                type.toFir(session)?.hasKotlinSuper(session, visited) == true
            }
            isInterface || origin == FirDeclarationOrigin.BuiltIns -> false
            else -> true
        }

    private fun ConeClassLikeType.toFir(session: FirSession): FirRegularClass? {
        val symbol = this.toSymbol(session)
        return if (symbol is FirRegularClassSymbol) {
            symbol.fir
        } else {
            null
        }
    }
}

private fun FirCallableSymbol<*>.isFromBuiltInClass(session: FirSession) =
    dispatchReceiverClassOrNull()?.toSymbol(session)?.fir?.origin == FirDeclarationOrigin.BuiltIns
