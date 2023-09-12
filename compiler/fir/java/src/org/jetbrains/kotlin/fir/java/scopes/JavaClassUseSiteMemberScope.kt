/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunctionCopy
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.synthetic.buildSyntheticProperty
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.declarations.buildJavaMethodCopy
import org.jetbrains.kotlin.fir.java.declarations.buildJavaValueParameterCopy
import org.jetbrains.kotlin.fir.java.resolveIfJavaType
import org.jetbrains.kotlin.fir.java.syntheticPropertiesStorage
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.ResultOfIntersection
import org.jetbrains.kotlin.fir.scopes.impl.MembersByScope
import org.jetbrains.kotlin.fir.scopes.impl.similarFunctionsOrBothProperties
import org.jetbrains.kotlin.fir.scopes.jvm.computeJvmDescriptor
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.*
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.ERASED_COLLECTION_PARAMETER_NAMES
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.sameAsBuiltinMethodWithErasedValueParameters
import org.jetbrains.kotlin.load.java.SpecialGenericSignatures.Companion.sameAsRenamedInJvmBuiltin
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class JavaClassUseSiteMemberScope(
    klass: FirJavaClass,
    session: FirSession,
    superTypeScopes: List<FirTypeScope>,
    declaredMemberScope: FirContainingNamesAwareScope
) : AbstractFirUseSiteMemberScope(
    klass.classId,
    session,
    JavaOverrideChecker(session, klass.javaTypeParameterStack, superTypeScopes, considerReturnTypeKinds = true),
    superTypeScopes,
    klass.defaultType(),
    declaredMemberScope
) {
    private val typeParameterStack = klass.javaTypeParameterStack

    private val canUseSpecialGetters: Boolean by lazy { !klass.hasKotlinSuper(session) }

    private val javaOverrideChecker: JavaOverrideChecker get() = overrideChecker as JavaOverrideChecker

    private val syntheticPropertyCache = session.syntheticPropertiesStorage.cacheByOwner.getValue(klass, null)

    private fun generateSyntheticPropertySymbol(
        getterSymbol: FirNamedFunctionSymbol,
        setterSymbol: FirNamedFunctionSymbol?,
        property: FirProperty,
        takeModalityFromGetter: Boolean,
    ): FirSyntheticPropertySymbol {
        return buildSyntheticProperty {
            moduleData = session.moduleData
            name = property.name
            symbol = FirJavaOverriddenSyntheticPropertySymbol(
                getterId = getterSymbol.callableId,
                propertyId = CallableId(getterSymbol.callableId.packageName, getterSymbol.callableId.className, property.name)
            )
            delegateGetter = getterSymbol.fir
            delegateSetter = setterSymbol?.fir
            status = getterSymbol.fir.status.copy(
                modality = if (takeModalityFromGetter) {
                    delegateGetter.modality ?: property.modality
                } else {
                    chooseModalityForAccessor(property, delegateGetter)
                }
            )
            deprecationsProvider = getDeprecationsProviderFromAccessors(session, delegateGetter, delegateSetter)
        }.symbol
    }

    private fun chooseModalityForAccessor(property: FirProperty, getter: FirSimpleFunction): Modality? {
        val a = property.modality
        val b = getter.modality

        if (a == null) return b
        if (b == null) return a

        return minOf(a, b)
    }

    override fun collectProperties(name: Name): Collection<FirVariableSymbol<*>> {
        val fields = mutableSetOf<FirCallableSymbol<*>>()
        val fieldNames = mutableSetOf<Name>()
        val result = mutableSetOf<FirVariableSymbol<*>>()

        // fields
        declaredMemberScope.processPropertiesByName(name) processor@{ variableSymbol ->
            if (variableSymbol.isStatic) return@processor
            fields += variableSymbol
            fieldNames += variableSymbol.fir.name
            result += variableSymbol
        }

        /*
         * From supertype we can get at most two results:
         * 1. Set of properties with same name
         * 2. Field from some java superclass (only one, if class have more than one superclass then we can choose
         *   just one field because this is incorrect code anyway)
         */
        val fromSupertypes = supertypeScopeContext.collectIntersectionResultsForCallables(name, FirScope::processPropertiesByName)

        val (fieldsFromSupertype, propertiesFromSupertypes) = fromSupertypes.partition {
            it is ResultOfIntersection.SingleMember && it.chosenSymbol is FirFieldSymbol
        }

        assert(fieldsFromSupertype.size in 0..1)
        assert(propertiesFromSupertypes.size in 0..1)

        fieldsFromSupertype.firstOrNull()?.chosenSymbol?.let { fieldSymbol ->
            require(fieldSymbol is FirFieldSymbol)
            if (fieldSymbol.name !in fieldNames) {
                result.addIfNotNull(fieldSymbol)
            }
        }

        @Suppress("UNCHECKED_CAST")
        val overriddenProperty = propertiesFromSupertypes.firstOrNull() as ResultOfIntersection<FirPropertySymbol>? ?: return result
        val overrideInClass = syntheticPropertyCache.getValue(name, this to overriddenProperty)

        val chosenSymbol = overrideInClass ?: overriddenProperty.chosenSymbol
        directOverriddenProperties[chosenSymbol] = listOf(overriddenProperty)
        overriddenProperty.overriddenMembers.forEach { overrideByBase[it.member] = overrideInClass }
        result += chosenSymbol
        return result
    }

    internal fun syntheticPropertyFromOverride(overriddenProperty: ResultOfIntersection<FirPropertySymbol>): FirSyntheticPropertySymbol? {
        val overrideInClass = overriddenProperty.overriddenMembers.firstNotNullOfOrNull { (symbol, _) ->
            symbol.createOverridePropertyIfExists(declaredMemberScope, takeModalityFromGetter = true)
                ?: superTypeScopes.firstNotNullOfOrNull { scope ->
                    symbol.createOverridePropertyIfExists(scope, takeModalityFromGetter = false)
                }
        }
        return overrideInClass
    }

    private fun FirPropertySymbol.createOverridePropertyIfExists(
        scope: FirScope,
        takeModalityFromGetter: Boolean
    ): FirSyntheticPropertySymbol? {
        val getterSymbol = this.findGetterOverride(scope) ?: return null
        val setterSymbol =
            if (this.fir.isVar)
                this.findSetterOverride(scope) ?: return null
            else
                null
        if (setterSymbol != null && setterSymbol.fir.modality != getterSymbol.fir.modality) return null

        return generateSyntheticPropertySymbol(getterSymbol, setterSymbol, fir, takeModalityFromGetter)
    }

    private fun FirPropertySymbol.findGetterOverride(
        scope: FirScope,
    ): FirNamedFunctionSymbol? {
        val specialGetterName = if (canUseSpecialGetters) getBuiltinSpecialPropertyGetterName() else null
        val name = specialGetterName?.asString() ?: JvmAbi.getterName(fir.name.asString())
        return findGetterByName(name, scope)
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
                when {
                    candidate.isAcceptableAsAccessorOverride() ->
                        // TODO: Decide something for the case when property type is not computed yet
                        expectedReturnType == null ||
                                AbstractTypeChecker.isSubtypeOf(session.typeContext, candidateReturnType, expectedReturnType)
                    else -> false
                }
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
                candidate.isAcceptableAsAccessorOverride() && AbstractTypeChecker.equalTypes(
                    session.typeContext, parameterType, propertyType
                )
            }
        }
    }

    private fun FirSimpleFunction.isAcceptableAsAccessorOverride(): Boolean {
        // We don't accept here accessors with type parameters from Kotlin to avoid strange cases like KT-59038
        // However, we (temporarily, see below) accept accessors from Kotlin in general to keep K1 compatibility in cases like KT-59550
        // KT-59601: we are going to forbid accessors from Kotlin in general after some investigation and/or deprecation period
        return isJavaOrEnhancement || typeParameters.isEmpty()
    }

    private fun FirPropertySymbol.getBuiltinSpecialPropertyGetterName(): Name? {
        var result: Name? = null
        superTypeScopes.processOverriddenPropertiesAndSelf(this) { overridden ->
            val fqName = overridden.fir.containingClassLookupTag()?.classId?.asSingleFqName()?.child(overridden.fir.name)

            BuiltinSpecialProperties.PROPERTY_FQ_NAME_TO_JVM_GETTER_NAME_MAP[fqName]?.let { name ->
                result = name
                return@processOverriddenPropertiesAndSelf ProcessorAction.STOP
            }

            ProcessorAction.NEXT
        }

        return result
    }

    // ---------------------------------------------------------------------------------------------------------

    override fun FirNamedFunctionSymbol.isVisibleInCurrentClass(): Boolean {
        val potentialPropertyNames = getPropertyNamesCandidatesByAccessorName(name)
        val hasCorrespondingProperty = potentialPropertyNames.any { propertyName ->
            getProperties(propertyName).any l@{ propertySymbol ->
                // TODO: add magic overrides from LazyJavaClassMemberScope.isVisibleAsFunctionInCurrentClass
                if (propertySymbol !is FirPropertySymbol) return@l false
                propertySymbol.isOverriddenInClassBy(this)
            }
        }
        if (hasCorrespondingProperty) return false

        return !doesOverrideRenamedBuiltins() &&
                !shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters() &&
                !doesOverrideSuspendFunction()
    }

    private fun FirNamedFunctionSymbol.doesOverrideRenamedBuiltins(): Boolean {
        // e.g. 'removeAt' or 'toInt'
        val builtinName = SpecialGenericSignatures.getBuiltinFunctionNamesByJvmName(name) ?: return false
        val builtinSpecialFromSuperTypes = supertypeScopeContext.collectMembersGroupedByScope(builtinName, FirScope::processFunctionsByName)
            .flatMap { (scope, symbols) ->
                symbols.filter { it.doesOverrideBuiltinWithDifferentJvmName(scope, session) }
            }
        if (builtinSpecialFromSuperTypes.isEmpty()) return false

        return builtinSpecialFromSuperTypes.any {
            // Here `this` and `it` have different names but it's ok because override checker does not consider
            //   names of declarations at all
            overrideChecker.isOverriddenFunction(this.fir, it.fir)
        }
    }

    /**
     * Checks if function is a valid override of JDK analogue of built-in method with erased value parameters (e.g. Map.containsKey(k: K))
     *
     * Examples:
     * - boolean containsKey(Object key) -> true
     * - boolean containsKey(K key) -> false // Wrong JDK method override, while it's a valid Kotlin built-in override
     *
     * There is a case when we shouldn't hide a function even if it overrides builtin member with value parameter erasure:
     *   if substituted kotlin overridden has the same parameters as current java override. Such situation may happen only in
     *   case when `Any`/`Object` is used as parameterization of supertype:
     *
     * // java
     * class MySuperMap extends java.util.Map<Object, Object> {
     *     @Override
     *     public boolean containsKey(Object key) {...}
     *
     *     @Override
     *     public boolean containsValue(Object key) {...}
     * }
     *
     * In this case, the signature of override, made based on the correct kotlin signature, will be the same (because of { K -> Any, V -> Any }
     *   substitution for both functions).
     * And since the list of all such functions is well-known, the only case when this may happen is when value parameter types of kotlin
     *   overridden are `Any`
     */
    private fun FirNamedFunctionSymbol.shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters(): Boolean {
        if (!name.sameAsBuiltinMethodWithErasedValueParameters) return false
        val candidatesToOverride = supertypeScopeContext.collectIntersectionResultsForCallables(name, FirScope::processFunctionsByName)
            .flatMap { it.overriddenMembers }
            .filterNot { (member, _) ->
                member.valueParameterSymbols.all { it.resolvedReturnType.lowerBoundIfFlexible().isAny }
            }.mapNotNull { (member, scope) ->
                BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(member, scope)
            }

        val jvmDescriptor = fir.computeJvmDescriptor()
        return candidatesToOverride.any { candidate ->
            candidate.fir.computeJvmDescriptor() == jvmDescriptor && this.hasErasedParameters()
        }
    }

    private fun FirNamedFunctionSymbol.doesOverrideSuspendFunction(): Boolean {
        val suspendView = createSuspendView() ?: return false
        return superTypeScopes.any { scope ->
            scope.getFunctions(name).any { it.isSuspend && overrideChecker.isOverriddenFunction(suspendView, it.fir) }
        }
    }

    private fun FirNamedFunctionSymbol.createSuspendView(): FirSimpleFunction? {
        val continuationParameter = fir.valueParameters.lastOrNull() ?: return null
        val owner = classId.toSymbol(session)?.fir as? FirJavaClass ?: return null
        val continuationParameterType = continuationParameter
            .returnTypeRef
            .resolveIfJavaType(session, owner.javaTypeParameterStack)
            .coneTypeSafe<ConeKotlinType>()
            ?.lowerBoundIfFlexible() as? ConeClassLikeType
            ?: return null
        if (continuationParameterType.lookupTag.classId.asSingleFqName() != StandardNames.CONTINUATION_INTERFACE_FQ_NAME) return null

        return buildSimpleFunctionCopy(fir) {
            valueParameters.clear()
            valueParameters.addAll(fir.valueParameters.dropLast(1))
            returnTypeRef = buildResolvedTypeRef {
                type = continuationParameterType.typeArguments[0].type ?: return null
            }
            (status as FirDeclarationStatusImpl).isSuspend = true
            symbol = FirNamedFunctionSymbol(callableId)
        }
    }

    // ---------------------------------------------------------------------------------------------------------

    override fun collectFunctions(name: Name): Collection<FirNamedFunctionSymbol> {
        val result = mutableListOf<FirNamedFunctionSymbol>()
        collectDeclaredFunctions(name, result)
        val explicitlyDeclaredFunctions = result.toSet()

        val functionsWithScopeFromSupertypes = supertypeScopeContext.collectMembersGroupedByScope(name, FirScope::processFunctionsByName)

        if (
            !name.sameAsRenamedInJvmBuiltin &&
            !name.sameAsBuiltinMethodWithErasedValueParameters &&
            functionsWithScopeFromSupertypes.all { it.second.none { functionSymbol -> functionSymbol.isSuspend } }
        ) {
            // Simple fast path in case of name is not suspicious (i.e. name is not one of builtins that have different signature in Java)
            super.collectFunctionsFromSupertypes(name, result, explicitlyDeclaredFunctions)
            return result
        }

        processSpecialFunctions(name, explicitlyDeclaredFunctions, functionsWithScopeFromSupertypes, result)
        return result.toSet()
    }

    private fun processSpecialFunctions(
        requestedName: Name,
        explicitlyDeclaredFunctionsWithNaturalName: Collection<FirNamedFunctionSymbol>,
        functionsFromSupertypesWithRequestedName: MembersByScope<FirNamedFunctionSymbol>, // candidates for override
        destination: MutableCollection<FirNamedFunctionSymbol>
    ) {
        val resultsOfIntersectionToSaveInCache = mutableListOf<ResultOfIntersection<FirNamedFunctionSymbol>>()
        val intersectionResults =
            supertypeScopeContext.convertGroupedCallablesToIntersectionResults(functionsFromSupertypesWithRequestedName)
        for (resultOfIntersectionWithNaturalName in intersectionResults) {
            val someSymbolWithNaturalNameFromSuperType = resultOfIntersectionWithNaturalName.extractSomeSymbolFromSuperType()
            val explicitlyDeclaredFunctionWithNaturalName = explicitlyDeclaredFunctionsWithNaturalName.firstOrNull {
                overrideChecker.isOverriddenFunction(it, someSymbolWithNaturalNameFromSuperType)
            }
            val jvmName = resultOfIntersectionWithNaturalName.overriddenMembers.firstNotNullOfOrNull {
                it.member.getJvmMethodNameIfSpecial(it.baseScope, session)
            }
            if (jvmName != null) {
                processOverridesForFunctionsWithDifferentJvmName(
                    jvmName,
                    someSymbolWithNaturalNameFromSuperType,
                    explicitlyDeclaredFunctionWithNaturalName,
                    requestedName,
                    resultOfIntersectionWithNaturalName,
                    destination,
                    resultsOfIntersectionToSaveInCache
                )
                continue
            }

            if (processOverridesForFunctionsWithErasedValueParameter(
                    resultOfIntersectionWithNaturalName.overriddenMembers,
                    requestedName,
                    explicitlyDeclaredFunctionsWithNaturalName,
                    destination,
                    resultOfIntersectionWithNaturalName,
                    explicitlyDeclaredFunctionWithNaturalName
                )
            ) continue

            // regular rules
            when (explicitlyDeclaredFunctionWithNaturalName) {
                null -> {
                    val chosenSymbol = resultOfIntersectionWithNaturalName.chosenSymbol
                    if (!chosenSymbol.isVisibleInCurrentClass()) continue
                    destination += chosenSymbol
                    resultsOfIntersectionToSaveInCache += resultOfIntersectionWithNaturalName
                }
                else -> {
                    destination += explicitlyDeclaredFunctionWithNaturalName
                    directOverriddenFunctions[explicitlyDeclaredFunctionWithNaturalName] = listOf(resultOfIntersectionWithNaturalName)
                    for (overriddenMember in resultOfIntersectionWithNaturalName.overriddenMembers) {
                        overrideByBase[overriddenMember.member] = explicitlyDeclaredFunctionWithNaturalName
                    }
                }
            }
        }
        functionsFromSupertypes[requestedName] = resultsOfIntersectionToSaveInCache
    }

    private fun processOverridesForFunctionsWithErasedValueParameter(
        overriddenMembers: List<MemberWithBaseScope<FirNamedFunctionSymbol>>,
        naturalName: Name,
        explicitlyDeclaredFunctionsWithNaturalName: Collection<FirNamedFunctionSymbol>,
        destination: MutableCollection<FirNamedFunctionSymbol>,
        resultOfIntersectionWithNaturalName: ResultOfIntersection<FirNamedFunctionSymbol>,
        explicitlyDeclaredFunctionWithNaturalName: FirNamedFunctionSymbol?
    ): Boolean {
        val overriddenMemberWithErasedValueParameters = overriddenMembers.firstOrNull {
            BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(it) != null
        }?.member ?: return false

        val unwrappedSubstitutionOverride = overriddenMemberWithErasedValueParameters.fir.originalForSubstitutionOverride
            ?: overriddenMemberWithErasedValueParameters.fir

        val functionFromSupertypeWithErasedParameterType = unwrappedSubstitutionOverride
            .initialSignatureAttr
            ?.symbol as? FirNamedFunctionSymbol
            ?: unwrappedSubstitutionOverride.symbol
        val originalDeclaredFunction = declaredMemberScope.getFunctions(naturalName).firstOrNull {
            it.hasSameJvmDescriptor(functionFromSupertypeWithErasedParameterType) && it.hasErasedParameters() &&
                    javaOverrideChecker.doesReturnTypesHaveSameKind(functionFromSupertypeWithErasedParameterType.fir, it.fir)
        } ?: return false
        /*
         * See the comment to shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters function
         * It explains why we should check value parameters for `Any` type
         */
        var allParametersAreAny = true
        val renamedDeclaredFunction = buildJavaMethodCopy(originalDeclaredFunction.fir as FirJavaMethod) {
            name = naturalName
            symbol = FirNamedFunctionSymbol(originalDeclaredFunction.callableId)
            this.valueParameters.clear()
            originalDeclaredFunction.fir.valueParameters.zip(overriddenMemberWithErasedValueParameters.fir.valueParameters)
                .mapTo(this.valueParameters) { (overrideParameter, parameterFromSupertype) ->
                    if (!parameterFromSupertype.returnTypeRef.coneType.lowerBoundIfFlexible().isAny) {
                        allParametersAreAny = false
                    }
                    buildJavaValueParameterCopy(overrideParameter) {
                        this@buildJavaValueParameterCopy.returnTypeRef = parameterFromSupertype.returnTypeRef
                    }
                }
        }.apply {
            initialSignatureAttr = originalDeclaredFunction.fir
        }.symbol

        if (allParametersAreAny) {
            return false
        }

        val hasAccidentalOverrideWithDeclaredFunction = explicitlyDeclaredFunctionsWithNaturalName.any {
            overrideChecker.isOverriddenFunction(
                renamedDeclaredFunction,
                it
            )
        }
        if (!hasAccidentalOverrideWithDeclaredFunction) {
            destination += renamedDeclaredFunction
            directOverriddenFunctions[renamedDeclaredFunction] = listOf(resultOfIntersectionWithNaturalName)
            for (overriddenMember in overriddenMembers) {
                overrideByBase[overriddenMember.member] = renamedDeclaredFunction
            }
            if (explicitlyDeclaredFunctionWithNaturalName != null) {
                destination += explicitlyDeclaredFunctionWithNaturalName
            }
            return true
        }
        return false
    }

    private fun FirNamedFunctionSymbol.hasSameJvmDescriptor(
        builtinWithErasedParameters: FirNamedFunctionSymbol
    ): Boolean {
        return fir.computeJvmDescriptor(includeReturnType = false) == builtinWithErasedParameters.fir.computeJvmDescriptor(includeReturnType = false)
    }

    private fun processOverridesForFunctionsWithDifferentJvmName(
        jvmName: Name,
        someSymbolWithNaturalNameFromSuperType: FirNamedFunctionSymbol,
        explicitlyDeclaredFunctionWithNaturalName: FirNamedFunctionSymbol?,
        naturalName: Name,
        resultOfIntersectionWithNaturalName: ResultOfIntersection<FirNamedFunctionSymbol>,
        destination: MutableCollection<FirNamedFunctionSymbol>,
        resultsOfIntersectionToSaveInCache: MutableList<ResultOfIntersection<FirNamedFunctionSymbol>>
    ) {
        /*
         * name: toByte
         *
         * 1. find declared byteValue (a)
         * 2. find toByte in supertypes (b)
         * 3. find byteValue in supertypes (c)
         * 4. create triples of (a), (b), (c) which are same overrides
         * 5. for each triple:
         * 6.   if (a) is empty:
         * 6.1. create renamed copies of (c): (c')
         * 6.2. create result of intersection for (b) and (c'), save direct overrides
         * 7.  if (a) is not empty:
         * 7.1 create renamed copies of (c): (c')
         * 7.2 save direct overrides
         */
        val overriddenMembers = resultOfIntersectionWithNaturalName.overriddenMembers
        val explicitlyDeclaredFunctionWithBuiltinJvmName = declaredMemberScope.getFunctions(jvmName).firstOrNull {
            overrideChecker.isOverriddenFunction(it, someSymbolWithNaturalNameFromSuperType)
        }
        val functionsFromSupertypesWithBuiltinJvmName = supertypeScopeContext.collectFunctions(jvmName).firstOrNull {
            overrideChecker.similarFunctionsOrBothProperties(
                it.extractSomeSymbolFromSuperType(),
                someSymbolWithNaturalNameFromSuperType
            )
        }

        val declaredFunction = explicitlyDeclaredFunctionWithNaturalName ?: explicitlyDeclaredFunctionWithBuiltinJvmName?.let {
            val original = it.fir as FirJavaMethod
            buildJavaMethodCopy(original) {
                name = naturalName
                symbol = FirNamedFunctionSymbol(it.callableId.copy(callableName = naturalName))
                status = original.status.copy(isOperator = true)
            }.apply {
                initialSignatureAttr = original
            }.symbol
        }

        val renamedFunctionsFromSupertypes = functionsFromSupertypesWithBuiltinJvmName?.overriddenMembers?.map {
            val renamedFunction = buildSimpleFunctionCopy(it.member.fir) {
                name = naturalName
                symbol = FirNamedFunctionSymbol(it.member.callableId.copy(callableName = naturalName))
                origin = FirDeclarationOrigin.RenamedForOverride
            }.apply {
                initialSignatureAttr = it.member.fir
            }
            it.baseScope to listOf(renamedFunction.symbol)
        }

        val resultsOfIntersection = when (renamedFunctionsFromSupertypes) {
            null -> listOf(resultOfIntersectionWithNaturalName)
            else -> {
                val membersByScope = buildList {
                    overriddenMembers.mapTo(this) { it.baseScope to listOf(it.member) }
                    addAll(renamedFunctionsFromSupertypes)
                }
                supertypeScopeContext.convertGroupedCallablesToIntersectionResults(membersByScope)
            }
        }

        if (declaredFunction != null) {
            destination += declaredFunction
            directOverriddenFunctions[declaredFunction] = resultsOfIntersection
            for (resultOfIntersection in resultsOfIntersection) {
                for (overriddenMember in resultOfIntersection.overriddenMembers) {
                    overrideByBase[overriddenMember.member] = declaredFunction
                }
            }
        } else {
            for (resultOfIntersection in resultsOfIntersection) {
                destination += resultOfIntersection.chosenSymbol
            }
            resultsOfIntersectionToSaveInCache += resultsOfIntersection
        }
    }

    private fun FirPropertySymbol.isOverriddenInClassBy(functionSymbol: FirNamedFunctionSymbol): Boolean {
        if (rawStatus.visibility == Visibilities.Private) return false

        val accessorDescriptors = when (val fir = fir) {
            is FirSyntheticProperty -> {
                if (fir.getter.delegate.symbol == functionSymbol || fir.setter?.delegate?.symbol == functionSymbol) return true
                val getterDescriptor = fir.getter.delegate.computeJvmDescriptor(includeReturnType = false)
                val setterDescriptor = fir.setter?.delegate?.computeJvmDescriptor(includeReturnType = false)
                listOf(getterDescriptor to setterDescriptor)
            }
            else -> {
                val getterNames =
                    listOfNotNull(getJvmMethodNameIfSpecial(this@JavaClassUseSiteMemberScope, session))
                        .takeIf { it.isNotEmpty() }
                        ?: possibleGetMethodNames(fir.name)
                getterNames.map { getterName ->
                    val getterDescriptor = fir.computeJvmDescriptorForGetter(
                        customName = getterName.identifier,
                        includeReturnType = false
                    )
                    val setterDescriptor = runIf(isVar) {
                        fir.computeJvmDescriptorForSetter(
                            customName = setMethodName(getterName).identifier,
                            includeReturnType = false
                        )
                    }
                    getterDescriptor to setterDescriptor
                }
            }
        }

        val currentJvmDescriptor = functionSymbol.fir.computeJvmDescriptor(includeReturnType = false)

        val getterDescriptorMatches = accessorDescriptors.any { (getterJvmDescriptor, _) ->
            val gettersAreSame = currentJvmDescriptor == getterJvmDescriptor && run {
                val propertyType = this.fir.returnTypeRef.probablyJavaTypeRefToConeType()
                val functionType = functionSymbol.fir.returnTypeRef.probablyJavaTypeRefToConeType()
                functionType.isSubtypeOf(propertyType, session)
            }
            gettersAreSame
        }

        if (getterDescriptorMatches && this.isVal) return true

        val setterDescriptorMatches = accessorDescriptors.any { (_, setterJvmDescriptor) ->
            currentJvmDescriptor == setterJvmDescriptor
        }

        if (!setterDescriptorMatches) return false

        val (getterOverride, setterOverride) = when (getterDescriptorMatches) {
            true -> functionSymbol to findSetterOverride(this@JavaClassUseSiteMemberScope)
            false -> findGetterOverride(this@JavaClassUseSiteMemberScope) to functionSymbol
        }
        return getterOverride?.modality == setterOverride?.modality
    }

    private fun FirTypeRef.probablyJavaTypeRefToConeType(): ConeKotlinType {
        return when (this) {
            is FirJavaTypeRef -> toConeKotlinTypeProbablyFlexible(session, typeParameterStack)
            else -> coneType
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

    private fun FirProperty.computeJvmDescriptorForGetter(customName: String? = null, includeReturnType: Boolean = false): String {
        getter?.computeJvmDescriptor(customName, includeReturnType)?.let { return it }
        val syntheticGetter = FirDefaultPropertyGetter(
            source = null,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = returnTypeRef,
            visibility = status.visibility,
            propertySymbol = symbol,
            modality = status.modality ?: Modality.FINAL
        )
        return syntheticGetter.computeJvmDescriptor(customName, includeReturnType)
    }

    private fun FirProperty.computeJvmDescriptorForSetter(customName: String? = null, includeReturnType: Boolean = false): String {
        setter?.computeJvmDescriptor(customName, includeReturnType)?.let { return it }
        val syntheticSetter = FirDefaultPropertySetter(
            source = null,
            moduleData = moduleData,
            origin = origin,
            propertyTypeRef = returnTypeRef,
            visibility = status.visibility,
            propertySymbol = symbol,
            modality = status.modality ?: Modality.FINAL
        )
        return syntheticSetter.computeJvmDescriptor(customName, includeReturnType)
    }

    private fun FirFunction.computeJvmDescriptor(customName: String? = null, includeReturnType: Boolean = false): String {
        return computeJvmDescriptor(customName, includeReturnType) {
            it.toConeKotlinTypeProbablyFlexible(
                session,
                typeParameterStack
            )
        }
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

    override fun toString(): String {
        return "Java use site scope of $classId"
    }
}
