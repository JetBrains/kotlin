/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.scopes

import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.isVisibleInClass
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
import org.jetbrains.kotlin.fir.java.symbols.FirJavaOverriddenSyntheticPropertySymbol
import org.jetbrains.kotlin.fir.java.syntheticPropertiesStorage
import org.jetbrains.kotlin.fir.java.toConeKotlinTypeProbablyFlexible
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.AbstractFirUseSiteMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.FirTypeIntersectionScopeContext.ResultOfIntersection
import org.jetbrains.kotlin.fir.scopes.impl.MembersByScope
import org.jetbrains.kotlin.fir.scopes.impl.isIntersectionOverride
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
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * It's a kind of use-site scope specialized for a Java owner class. Provides access to symbols by names, both for
 * declared inside Java-class and inherited from its base classes (as usual for use-site scopes).
 *
 * * all functions that are explicitly declared in a Java class, are visible by default.
 * Exceptions: functions that have corresponding properties; functions that override renamed builtins (e.g. charAt);
 * functions that are overrides with erased type parameters (e.g. contains(Object)).
 * See [isVisibleInCurrentClass]. E.g. contains(String) or get(int) are visible as explicitly declared functions.
 *
 * * also, this scope as a use-site member scope shows us some functions from supertypes. Here we have two choices (see [collectFunctions]):
 *     * in the fast path, which is used when supertypes do not include any suspend functions AND the name is "standard"
 *     (we don't suspect possible builtin renaming or type parameter erasing), we use a standard procedure:
 *     class sees all supertype functions except those overridden by explicitly declared functions in the class
 *     * otherwise, [processSpecialFunctions] comes into play.
 *     It attempts to find a specific override for this "erased type parameter" and "renamed built-in" case.
 *     In case of success, it always creates a synthetic function and shows it as a scope part:
 *         * in case we have an "accidental normal override", like contains(String) or get(int),
 *         its "hidden" version is created as the synthetic function
 *         * in case we don't have it, we create an implicit function with such a signature (contains(String), get(int)) as the synthetic function
 */
class JavaClassUseSiteMemberScope(
    private val klass: FirJavaClass,
    session: FirSession,
    superTypeScopes: List<FirTypeScope>,
    declaredMemberScope: FirContainingNamesAwareScope
) : AbstractFirUseSiteMemberScope(
    klass.symbol.toLookupTag(),
    session,
    JavaOverrideChecker(session, klass.javaTypeParameterStack, superTypeScopes, considerReturnTypeKinds = true),
    overrideCheckerForIntersection = null,
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
         * From supertype we can get:
         * 1. Set of properties with same name (including regular and extension properties)
         * 2. Field from some java superclass (only one, if class have more than one superclass then we can choose
         *   just one field because this is incorrect code anyway)
         */
        val fromSupertypes = supertypeScopeContext.collectIntersectionResultsForCallables(name, FirScope::processPropertiesByName)

        val (fieldsFromSupertype, propertiesFromSupertypes) = fromSupertypes.partition {
            it is ResultOfIntersection.SingleMember && it.chosenSymbol is FirFieldSymbol
        }

        assert(fieldsFromSupertype.size in 0..1)

        fieldsFromSupertype.firstOrNull()?.chosenSymbol?.let { fieldSymbol ->
            require(fieldSymbol is FirFieldSymbol)
            if (fieldSymbol.name !in fieldNames) {
                result.add(fieldSymbol)
            }
        }

        @Suppress("UNCHECKED_CAST")
        for (overriddenProperty in propertiesFromSupertypes as List<ResultOfIntersection<FirPropertySymbol>>) {
            val overrideInClass = syntheticPropertyCache.getValue(name, this to overriddenProperty)

            val chosenSymbol = overrideInClass ?: overriddenProperty.chosenSymbol
            directOverriddenProperties[chosenSymbol] = listOf(overriddenProperty)
            overriddenProperty.overriddenMembers.forEach { overrideByBase[it.member] = overrideInClass }
            result += chosenSymbol
        }

        return result
    }

    internal fun syntheticPropertyFromOverride(overriddenProperty: ResultOfIntersection<FirPropertySymbol>): FirSyntheticPropertySymbol? {
        val overrideInClass = overriddenProperty.overriddenMembers.firstNotNullOfOrNull { (symbol, _) ->
            // We may call this function at the STATUS phase, which means that using resolved status may lead to cycle
            // So we need to use raw status here
            if (!symbol.isVisibleInClass(klass.symbol, symbol.rawStatus)) return@firstNotNullOfOrNull null
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
                // We may call this function at the STATUS phase, which means that using resolved status may lead to cycle
                //   so we need to use raw status here
                propertySymbol.isVisibleInClass(this@JavaClassUseSiteMemberScope.klass.symbol, propertySymbol.rawStatus) &&
                        propertySymbol.isOverriddenInClassBy(this)
            }
        }
        if (hasCorrespondingProperty) return false

        return !doesOverrideRenamedBuiltins() &&
                !shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters()
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

    override fun FirNamedFunctionSymbol.replaceWithWrapperSymbolIfNeeded(): FirNamedFunctionSymbol {
        if (!isJavaOrEnhancement) return this
        val continuationParameter = fir.valueParameters.lastOrNull() ?: return this
        val owner = ownerClassLookupTag.toSymbol(session)?.fir as? FirJavaClass ?: return this
        val continuationParameterType = continuationParameter
            .returnTypeRef
            .resolveIfJavaType(session, owner.javaTypeParameterStack)
            .coneTypeSafe<ConeKotlinType>()
            ?.lowerBoundIfFlexible() as? ConeClassLikeType
            ?: return this
        if (continuationParameterType.lookupTag.classId.asSingleFqName() != StandardNames.CONTINUATION_INTERFACE_FQ_NAME) return this

        return buildSimpleFunctionCopy(fir) {
            valueParameters.clear()
            valueParameters.addAll(fir.valueParameters.dropLast(1))
            returnTypeRef = buildResolvedTypeRef {
                type = continuationParameterType.typeArguments[0].type ?: return this@replaceWithWrapperSymbolIfNeeded
            }
            (status as FirDeclarationStatusImpl).isSuspend = true
            symbol = FirNamedFunctionSymbol(callableId)
        }.symbol
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

    private fun <D : FirCallableSymbol<*>> ResultOfIntersection<D>.extractSomeSymbolFromSuperType(): D {
        return if (this.isIntersectionOverride()) {
            /*
             * we don't want to create intersection override if some declared function actually overrides some functions
             *   from supertypes, so instead of intersection override symbol we check actual symbol from supertype
             *
             * TODO(KT-65925): is it enough to check only one function?
             */
            keySymbol
        } else {
            chosenSymbol
        }
    }

    private fun processSpecialFunctions(
        requestedName: Name,
        explicitlyDeclaredFunctionsWithNaturalName: Collection<FirNamedFunctionSymbol>,
        functionsFromSupertypesWithRequestedName: MembersByScope<FirNamedFunctionSymbol>, // candidates for override
        destination: MutableCollection<FirNamedFunctionSymbol>
    ) {
        val functionsFromSupertypesToSaveInCache = mutableListOf<ResultOfIntersection<FirNamedFunctionSymbol>>()
        // The special override checker is needed for the case when we're trying to consider e.g. explicitly defined `Long toLong()`
        // as an override of `long toLong()` which is an enhanced version of `long longValue()`. K1 in such cases used
        // LazyJavaClassMemberScope.doesOverride, that ignores the return type, so we reproduce the behavior here.
        // See the test testData/diagnostics/tests/j+k/kt62197.kt and the issue KT-62197 for more details.
        // TODO: consider some more transparent approach
        val overrideCheckerForSpecialFunctions =
            JavaOverrideChecker(session, klass.javaTypeParameterStack, superTypeScopes, considerReturnTypeKinds = false)
        val intersectionResults =
            supertypeScopeContext.convertGroupedCallablesToIntersectionResults(functionsFromSupertypesWithRequestedName)
        for (resultOfIntersectionWithNaturalName in intersectionResults) {
            val someSymbolWithNaturalNameFromSuperType = resultOfIntersectionWithNaturalName.extractSomeSymbolFromSuperType()
            val explicitlyDeclaredFunctionWithNaturalName = explicitlyDeclaredFunctionsWithNaturalName.firstOrNull {
                overrideCheckerForSpecialFunctions.isOverriddenFunction(it, someSymbolWithNaturalNameFromSuperType)
            }

            if (processOverridesForFunctionsWithDifferentJvmName(
                    someSymbolWithNaturalNameFromSuperType,
                    explicitlyDeclaredFunctionWithNaturalName,
                    requestedName,
                    resultOfIntersectionWithNaturalName,
                    destination,
                    functionsFromSupertypesToSaveInCache
                )
            ) {
                continue
            }

            if (processOverridesForFunctionsWithErasedValueParameter(
                    requestedName,
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
                    functionsFromSupertypesToSaveInCache += resultOfIntersectionWithNaturalName
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
        functionsFromSupertypes[requestedName] = functionsFromSupertypesToSaveInCache
    }

    /**
     * This function collects in [destination] an overriding method for base method group [resultOfIntersectionWithNaturalName],
     * in case base methods should have their value parameters erased in Java,
     * e.g. Collection.contains(T) in Kotlin is paired with Collection.contains(Object) in Java.
     *
     * Given we have a Java class [klass] and some its method(s) name [name]
     * with base method group [resultOfIntersectionWithNaturalName] and (maybe)
     * explicitly declared [explicitlyDeclaredFunctionWithNaturalName],
     * this function builds a synthetic override for [resultOfIntersectionWithNaturalName] in the Java class,
     * binds it with this intersection result using the override relation,
     * and collects it as a matching method with this name.
     *
     * Important: all explicitly declared functions are already collected at this point, there is no reason to collect them once more!
     *
     * @param name a given method name
     * @param destination used to collect base functions for [explicitlyDeclaredFunctionWithNaturalName] with erased value parameters in Java
     * @param resultOfIntersectionWithNaturalName one group of intersected base methods, each "overridden member" inside is a pair of (base method, its scope)
     * @param explicitlyDeclaredFunctionWithNaturalName the function in the Java class [klass] with the name [name], which overrides [resultOfIntersectionWithNaturalName] (if any)
     * @return true if we collected something, false otherwise
     * @see [SpecialGenericSignatures.GENERIC_PARAMETERS_METHODS_TO_DEFAULT_VALUES_MAP] and
     * [SpecialGenericSignatures.ERASED_COLLECTION_PARAMETER_NAME_AND_SIGNATURES]
     */
    private fun processOverridesForFunctionsWithErasedValueParameter(
        name: Name,
        destination: MutableCollection<FirNamedFunctionSymbol>,
        resultOfIntersectionWithNaturalName: ResultOfIntersection<FirNamedFunctionSymbol>,
        explicitlyDeclaredFunctionWithNaturalName: FirNamedFunctionSymbol?
    ): Boolean {
        val membersFromSupertypesWithScopes = resultOfIntersectionWithNaturalName.overriddenMembers
        // E.g. contains(String) or contains(T)
        val memberFromSupertypeWithValueParametersToBeErased = membersFromSupertypesWithScopes.firstOrNull { (member, scope) ->
            BuiltinMethodsWithSpecialGenericSignature.getOverriddenBuiltinFunctionWithErasedValueParametersInJava(member, scope) != null
        }?.member ?: return false

        // E.g. contains(T)
        val unwrappedMemberFromSupertypeWithValueParametersToBeErased =
            memberFromSupertypeWithValueParametersToBeErased.fir.originalForSubstitutionOverride
                ?: memberFromSupertypeWithValueParametersToBeErased.fir

        val functionFromSupertypeWithValueParametersToBeErased = unwrappedMemberFromSupertypeWithValueParametersToBeErased
            .initialSignatureAttr
            ?.symbol as? FirNamedFunctionSymbol
            ?: unwrappedMemberFromSupertypeWithValueParametersToBeErased.symbol

        // E.g. contains(Object) from Java
        val explicitlyDeclaredFunctionWithErasedValueParameters =
            declaredMemberScope.getFunctions(name).firstOrNull { declaredFunction ->
                declaredFunction.hasSameJvmDescriptor(functionFromSupertypeWithValueParametersToBeErased) &&
                        declaredFunction.hasErasedParameters() &&
                        javaOverrideChecker.doesReturnTypesHaveSameKind(
                            functionFromSupertypeWithValueParametersToBeErased.fir,
                            declaredFunction.fir
                        )
            } ?: return false // No declared functions with erased parameters => no additional processing needed

        /**
         * See the comment to [shouldBeVisibleAsOverrideOfBuiltInWithErasedValueParameters] function
         * It explains why we should check value parameters for `Any` type
         */
        var allParametersAreAny = true
        // It's a copy like contains(T) or contains(String) in Java, we perform "unerasing" here
        val declaredFunctionCopyWithParameterTypesFromSupertype = buildJavaMethodCopy(
            explicitlyDeclaredFunctionWithErasedValueParameters.fir as FirJavaMethod
        ) {
            this.name = name
            symbol = FirNamedFunctionSymbol(explicitlyDeclaredFunctionWithErasedValueParameters.callableId)
            this.valueParameters.clear()
            explicitlyDeclaredFunctionWithErasedValueParameters.fir.valueParameters.zip(
                memberFromSupertypeWithValueParametersToBeErased.fir.valueParameters
            ).mapTo(this.valueParameters) { (overrideParameter, parameterFromSupertype) ->
                if (!parameterFromSupertype.returnTypeRef.coneType.lowerBoundIfFlexible().isAny) {
                    allParametersAreAny = false
                }
                buildJavaValueParameterCopy(overrideParameter) {
                    this@buildJavaValueParameterCopy.returnTypeRef = parameterFromSupertype.returnTypeRef
                }
            }
        }.apply {
            initialSignatureAttr = explicitlyDeclaredFunctionWithErasedValueParameters.fir
        }.symbol

        if (allParametersAreAny) {
            return false
        }

        // E.g. contains(String) from Java, if any
        val accidentalOverrideWithDeclaredFunction = explicitlyDeclaredFunctionWithNaturalName?.takeIf {
            overrideChecker.isOverriddenFunction(
                declaredFunctionCopyWithParameterTypesFromSupertype,
                it
            )
        }
        val symbolToBeCollected = if (accidentalOverrideWithDeclaredFunction == null) {
            // Collect synthetic function which is an "unerased" copy of declared one with erased parameters
            declaredFunctionCopyWithParameterTypesFromSupertype
        } else {
            val newSymbol = FirNamedFunctionSymbol(accidentalOverrideWithDeclaredFunction.callableId)
            val original = accidentalOverrideWithDeclaredFunction.fir
            val accidentalOverrideWithDeclaredFunctionHiddenCopy = buildSimpleFunctionCopy(original) {
                this.name = name
                symbol = newSymbol
                dispatchReceiverType = klass.defaultType()
            }.apply {
                initialSignatureAttr = explicitlyDeclaredFunctionWithErasedValueParameters.fir
                isHiddenToOvercomeSignatureClash = true
            }
            // Collect synthetic function which is a hidden copy of declared one with unerased parameters
            accidentalOverrideWithDeclaredFunctionHiddenCopy.symbol
        }
        destination += symbolToBeCollected
        directOverriddenFunctions[symbolToBeCollected] = listOf(resultOfIntersectionWithNaturalName)
        for ((member, _) in membersFromSupertypesWithScopes) {
            overrideByBase[member] = symbolToBeCollected
        }
        return true
    }

    private fun FirNamedFunctionSymbol.hasSameJvmDescriptor(
        builtinWithErasedParameters: FirNamedFunctionSymbol
    ): Boolean {
        return fir.computeJvmDescriptor(includeReturnType = false) == builtinWithErasedParameters.fir.computeJvmDescriptor(includeReturnType = false)
    }

    /**
     * This function collects in [destination] an overriding method for base method group [resultOfIntersectionWithNaturalName],
     * in case base methods should have its name changed in Java,
     * e.g. MutableList.removeAt(Int) in Kotlin is paired with List.remove(int) in Java.
     *
     * Given we have a Java class [klass] and some its method(s) name mapped to [naturalName] in Kotlin
     * with base method group [resultOfIntersectionWithNaturalName] and (maybe)
     * explicitly declared [explicitlyDeclaredFunctionWithNaturalName],
     * this function builds a synthetic override for [resultOfIntersectionWithNaturalName] in the Java class,
     * binds it with this intersection result using the override relation,
     * and collects it as a matching method with this [naturalName].
     *
     * Important: all explicitly declared functions are already collected at this point, there is no reason to collect them once more!
     *
     * @param naturalName the Kotlin name of the function, e.g., toByte, get, removeAt
     * @param destination used to collect base functions for [explicitlyDeclaredFunctionWithNaturalName] with erased value parameters in Java
     * @param resultOfIntersectionWithNaturalName one group of intersected base methods, each "overridden member" inside is a pair of (base method, its scope)
     * @param someSymbolWithNaturalNameFromSuperType unwrapped symbol taken from [resultOfIntersectionWithNaturalName]
     * @param explicitlyDeclaredFunctionWithNaturalName the function in the Java class [klass] with the name [naturalName], which overrides [resultOfIntersectionWithNaturalName] (if any)
     * @return true if we collected something, false otherwise
     * @see [SpecialGenericSignatures.NAME_AND_SIGNATURE_TO_JVM_REPRESENTATION_NAME_MAP] and
     * [SpecialGenericSignatures.JVM_SIGNATURES_FOR_RENAMED_BUILT_INS]
     */
    private fun processOverridesForFunctionsWithDifferentJvmName(
        someSymbolWithNaturalNameFromSuperType: FirNamedFunctionSymbol,
        explicitlyDeclaredFunctionWithNaturalName: FirNamedFunctionSymbol?,
        naturalName: Name,
        resultOfIntersectionWithNaturalName: ResultOfIntersection<FirNamedFunctionSymbol>,
        destination: MutableCollection<FirNamedFunctionSymbol>,
        functionsFromSupertypesToSaveInCache: MutableList<ResultOfIntersection<FirNamedFunctionSymbol>>
    ): Boolean {
        // The JVM name of the function, e.g., byteValue or charAt
        val jvmName = resultOfIntersectionWithNaturalName.overriddenMembers.firstNotNullOfOrNull {
            it.member.getJvmMethodNameIfSpecial(it.baseScope, session)
        } ?: return false

        /**
         * naturalName: `toByte` (or: `CharSequence.get`, or: `MutableList.removeAt`)
         * jvmName: `byteValue` (or: `CharSequence.charAt`, or: `MutableList.remove`)
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

        // Among the overridden members, some can be regular members and some can be renamed from jvmName to naturalName.
        // If we have the CharBuffer situation, the visible member will override the regular ones and the hidden member will
        // override the renamed ones (if they exist).
        // The second part could be empty, but the first one cannot! See jvmName calculation above
        // Both parts must have name of naturalName
        // Example when both exist: testWeirdCharBuffers, class CharBufferXAllInherited : CharSequence, X
        // interface X in this example contains get(Int): Char
        val (intersectedOverridingRenamedBuiltin, intersectedOverridingNonBuiltin) =
            resultOfIntersectionWithNaturalName.overriddenMembers.partition {
                it.member.getJvmMethodNameIfSpecial(it.baseScope, session) == jvmName
            }

        val explicitlyDeclaredFunctionWithBuiltinJvmName = declaredMemberScope.getFunctions(jvmName).firstOrNull {
            overrideChecker.isOverriddenFunction(it, someSymbolWithNaturalNameFromSuperType)
        }
        val functionsFromSupertypesWithBuiltinJvmName = supertypeScopeContext.collectFunctions(jvmName).firstOrNull {
            overrideChecker.similarFunctionsOrBothProperties(
                it.extractSomeSymbolFromSuperType(),
                someSymbolWithNaturalNameFromSuperType
            )
        }

        fun createCopyWithNaturalName(
            originalSymbol: FirNamedFunctionSymbol,
            isHidden: Boolean = false,
            origin: FirDeclarationOrigin? = null,
        ): FirNamedFunctionSymbol {
            val original = originalSymbol.fir
            val newSymbol = FirNamedFunctionSymbol(originalSymbol.callableId.copy(callableName = naturalName))

            // If original is declared, it's a FirJavaMethod.
            // If it's inherited, it's a (possibly enhanced) FirSimpleMethod.
            return if (original is FirJavaMethod) {
                buildJavaMethodCopy(original) {
                    name = naturalName
                    symbol = newSymbol
                    dispatchReceiverType = klass.defaultType()

                    // Technically, it should only be an operator if it matches an operator naming convention,
                    // but always setting it doesn't seem to hurt.
                    status = original.status.copy(isOperator = true)
                }
            } else {
                buildSimpleFunctionCopy(original) {
                    name = naturalName
                    symbol = newSymbol
                    dispatchReceiverType = klass.defaultType()
                    origin?.let { this.origin = it }
                }
            }.apply {
                initialSignatureAttr = original
                if (isHidden) {
                    isHiddenToOvercomeSignatureClash = true
                }
            }.symbol
        }

        // Non-builtins with natural name are never here!
        val resultsOfIntersectionWithNaturalNameOrRenamed = when {
            // Something with jvmNames in supertypes: intersect them and renamed builtins, excluding non-builtins with natural name
            functionsFromSupertypesWithBuiltinJvmName != null -> {
                val membersByScope = buildList {
                    intersectedOverridingRenamedBuiltin.mapTo(this) { it.baseScope to listOf(it.member) }
                    addAll(
                        functionsFromSupertypesWithBuiltinJvmName.overriddenMembers.map {
                            val renamedFunction = createCopyWithNaturalName(it.member, origin = FirDeclarationOrigin.RenamedForOverride)
                            it.baseScope to listOf(renamedFunction)
                        }
                    )
                }
                supertypeScopeContext.convertGroupedCallablesToIntersectionResults(membersByScope)
            }
            // Nothing with jvmNames in supertype
            // Intersect only renamed builtins, excluding non-builtins with natural name, if any
            else -> {
                // We could leave only else branch here, if branch is just an optimization
                if (intersectedOverridingNonBuiltin.isEmpty()) listOf(resultOfIntersectionWithNaturalName)
                else supertypeScopeContext.convertGroupedCallablesToIntersectionResults(
                    intersectedOverridingRenamedBuiltin.map { it.baseScope to listOf(it.member) }
                )
            }
        }

        val functionWithNaturalNameExists = explicitlyDeclaredFunctionWithNaturalName != null ||
                intersectedOverridingNonBuiltin.isNotEmpty()
        if (explicitlyDeclaredFunctionWithBuiltinJvmName != null) {
            // If `charAt(Int): Char` is declared, we create its copy as `get(Int): Char`.
            // It should be hidden in case we already have another `get(Int): Char`
            val renamedFunction = createCopyWithNaturalName(
                explicitlyDeclaredFunctionWithBuiltinJvmName, isHidden = functionWithNaturalNameExists
            )
            destination += renamedFunction
            setOverrides(renamedFunction, resultsOfIntersectionWithNaturalNameOrRenamed)
        }
        if (functionWithNaturalNameExists) {
            // The CharBuffer situation as example: both `get(Int):Char` and `charAt(Int):Char` are declared or inherited.

            val resultOfIntersectionOfOverridingNonBuiltin = supertypeScopeContext.convertGroupedCallablesToIntersectionResults(
                intersectedOverridingNonBuiltin.map { it.baseScope to listOf(it.member) }
            )

            if (explicitlyDeclaredFunctionWithNaturalName != null) {
                // We have declared `get(Int): Char` => we need to update its overridden declarations.
                setOverrides(
                    explicitlyDeclaredFunctionWithNaturalName,
                    when {
                        // We have only explicit `get(Int): Char` but no explicit `charAt(Int): Char`
                        // Build overrides by including renamed functionsFromSupertypesWithBuiltinJvmName and renamed built-ins,
                        // but excluding non-builtins with natural name
                        explicitlyDeclaredFunctionWithBuiltinJvmName == null -> resultsOfIntersectionWithNaturalNameOrRenamed
                        // There is also an explicit `charAt(Int): Char`
                        // Then explicit `get(Int): Char` mustn't override `kotlin.CharSequence.get`,
                        // but it can override non-builtin declarations with natural name.
                        // See compiler/testData/diagnostics/tests/j+k/collectionOverrides/charBuffer.kt
                        else -> resultOfIntersectionOfOverridingNonBuiltin
                    }
                )
            } else {
                // `get(Int): Char` is inherited (possibly a real intersection).
                // Add it to destination and set overridden declarations.
                val intersectionOfNaturalName = resultOfIntersectionOfOverridingNonBuiltin.single()
                destination += intersectionOfNaturalName.chosenSymbol
                if (intersectionOfNaturalName is ResultOfIntersection.NonTrivial) {
                    setOverrides(intersectionOfNaturalName.chosenSymbol, resultOfIntersectionOfOverridingNonBuiltin)
                }
            }
        } else if (explicitlyDeclaredFunctionWithBuiltinJvmName == null) {
            // No functions with naturalName, like `get(Int): Char`, are in scope
            // Functions with jvmMame, like `charAt(Int): Char`, come from supertypes only
            for (resultOfIntersection in resultsOfIntersectionWithNaturalNameOrRenamed) {
                destination += resultOfIntersection.chosenSymbol
            }
            functionsFromSupertypesToSaveInCache += resultsOfIntersectionWithNaturalNameOrRenamed
        }

        return true
    }

    private fun setOverrides(override: FirNamedFunctionSymbol, overridden: List<ResultOfIntersection<FirNamedFunctionSymbol>>) {
        directOverriddenFunctions[override] = overridden
        for (resultOfIntersection in overridden) {
            for (overriddenMember in resultOfIntersection.overriddenMembers) {
                overrideByBase[overriddenMember.member] = override
            }
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
        // We're only interested in Java declarations with erased parameters.
        // Removing this check would also return true for substitution overrides for raw types,
        // which leads to false positives like NOTHING_TO_OVERRIDE.
        // See compiler/testData/diagnostics/tests/rawTypes/rawTypeOverrides.kt.
        if (!this.isJavaOrEnhancement) return false

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
        return "Java use site scope of ${ownerClassLookupTag.classId}"
    }
}
