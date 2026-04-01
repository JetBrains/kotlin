/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.ReturnValueCheckerMode
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher.matchSingleExpectAgainstPotentialActuals
import org.jetbrains.kotlin.resolve.checkers.OptInNames
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualIncompatibility
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.resolve.multiplatform.MemberIncompatibility
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker
import org.jetbrains.kotlin.utils.addToStdlib.enumMapOf
import org.jetbrains.kotlin.utils.addToStdlib.enumSetOf
import org.jetbrains.kotlin.utils.zipIfSizesAreEqual
import java.util.*

/**
 * This object is responsible for checking of expect-actual pairs
 * after they have been matched by [the matcher][AbstractExpectActualMatcher]
 *
 * See `/docs/fir/k2_kmp.md` for details
 */
object AbstractExpectActualChecker {
    fun <T : DeclarationSymbolMarker> getClassifiersCompatibility(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassLikeSymbol: ClassLikeSymbolMarker,
        context: ExpectActualMatchingContext<T>,
        languageVersionSettings: LanguageVersionSettings,
    ): List<ExpectActualIncompatibility<T>> {
        val result = with(context) {
            getClassifiersCompatibility(
                expectClassSymbol,
                actualClassLikeSymbol,
                parentSubstitutor = null,
                languageVersionSettings,
            )
        }
        @Suppress("UNCHECKED_CAST")
        return result as List<ExpectActualIncompatibility<T>>
    }

    fun <T : DeclarationSymbolMarker> getCallablesCompatibility(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        expectContainingClass: RegularClassSymbolMarker?,
        actualContainingClass: RegularClassSymbolMarker?,
        context: ExpectActualMatchingContext<T>,
        languageVersionSettings: LanguageVersionSettings,
    ): List<ExpectActualIncompatibility<T>> = with(context) {
        val expectTypeParameters = expectContainingClass?.typeParameters.orEmpty()
        val actualTypeParameters = actualContainingClass?.typeParameters.orEmpty()
        val parentSubstitutor = (expectTypeParameters zipIfSizesAreEqual actualTypeParameters)
            ?.let { createExpectActualTypeParameterSubstitutor(it, parentSubstitutor = null) }
        val result = getCallablesCompatibility(
            expectDeclaration,
            actualDeclaration,
            parentSubstitutor,
            expectContainingClass,
            actualContainingClass,
            languageVersionSettings,
        )
        @Suppress("UNCHECKED_CAST")
        result as List<ExpectActualIncompatibility<T>>
    }

    fun <T : DeclarationSymbolMarker> checkSingleExpectTopLevelDeclarationAgainstMatchedActual(
        expectDeclaration: DeclarationSymbolMarker,
        actualDeclaration: DeclarationSymbolMarker,
        context: ExpectActualMatchingContext<T>,
        languageVersionSettings: LanguageVersionSettings,
    ) {
        with(context) {
            checkSingleExpectAgainstMatchedActual(
                expectDeclaration,
                actualDeclaration,
                substitutor = null,
                expectClassSymbol = null,
                actualClassSymbol = null,
                incompatibleMembers = null,
                languageVersionSettings,
            )
        }
    }

    private fun ExpectActualMatchingContext<*>.isInterfaceActualizedAsAny(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
    ) = expectClassSymbol.classKind == ClassKind.INTERFACE && actualClassSymbol.defaultType.typeConstructor().isAnyConstructor()

    private fun ExpectActualMatchingContext<*>.getClassifiersCompatibility(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassLikeSymbol: ClassLikeSymbolMarker,
        parentSubstitutor: TypeSubstitutorMarker?,
        languageVersionSettings: LanguageVersionSettings,
    ): List<ExpectActualIncompatibility<*>> = buildList {
        // Can't check FQ names here because nested expected class may be implemented via actual typealias's expansion with the other FQ name
        require(nameOf(expectClassSymbol) == nameOf(actualClassLikeSymbol)) {
            "This function should be invoked only for declarations with the same name: $expectClassSymbol, $actualClassLikeSymbol"
        }

        val actualClass = computeActualClassFromPotentialActualTypealias(
            expectClassSymbol,
            actualClassLikeSymbol,
            onNestedTypealias = { return@getClassifiersCompatibility listOf(ExpectActualIncompatibility.NestedTypeAlias) },
            // do not report extra error on erroneous typealias
            onErroneousTypealias = { return@getClassifiersCompatibility emptyList() },
        )!!

        val allowUsingAnyAsActualInterface =
            languageVersionSettings.supportsFeature(LanguageFeature.AllowAnyAsAnActualTypeForExpectInterface)

        if (!areCompatibleClassKinds(expectClassSymbol, actualClass)) {
            if (!allowUsingAnyAsActualInterface || !isInterfaceActualizedAsAny(expectClassSymbol, actualClass)) {
                add(ExpectActualIncompatibility.ClassKind)
            }
        } else {
            // Don't report modality mismatch when classifiers don't match by ClassKind.
            // Different classifiers might have different modality (e.g. interface vs class)
            if (!areCompatibleModalities(expectClassSymbol.modality, actualClass.modality)) {
                add(ExpectActualIncompatibility.Modality(expectClassSymbol.modality, actualClass.modality))
            }
        }

        if (!equalBy(expectClassSymbol, actualClass) { listOf(it.isCompanion, it.isInner, it.isInlineOrValue) }) {
            add(ExpectActualIncompatibility.ClassModifiers)
        }

        if (expectClassSymbol.isFun && (!actualClass.isFun || !actualClass.isSamInterface())) {
            add(ExpectActualIncompatibility.FunInterfaceModifier)
        }

        val expectTypeParameterSymbols = expectClassSymbol.typeParameters
        val actualTypeParameterSymbols = actualClass.typeParameters
        if (expectTypeParameterSymbols.size != actualTypeParameterSymbols.size) {
            add(ExpectActualIncompatibility.ClassTypeParameterCount)
        }

        if (!areCompatibleClassVisibilities(expectClassSymbol, actualClass)) {
            add(ExpectActualIncompatibility.Visibility)
        }

        val substitutor = (expectTypeParameterSymbols zipIfSizesAreEqual actualTypeParameterSymbols)?.let {
            createExpectActualTypeParameterSubstitutor(it, parentSubstitutor)
        }

        if (substitutor != null && !areCompatibleTypeParameterUpperBounds(expectTypeParameterSymbols, actualTypeParameterSymbols, substitutor)) {
            add(ExpectActualIncompatibility.ClassTypeParameterUpperBounds)
        }

        getTypeParametersVarianceOrReifiedIncompatibility(expectTypeParameterSymbols, actualTypeParameterSymbols)
            ?.let { add(it) }

        if (substitutor != null && !areCompatibleSupertypes(expectClassSymbol, actualClass, substitutor)) {
            add(ExpectActualIncompatibility.Supertypes)
        }

        if (isIllegalRequiresOptInAnnotation(on = actualClass, expectClassSymbol, languageVersionSettings)) {
            add(ExpectActualIncompatibility.IllegalRequiresOpt)
        }

        if (substitutor != null) {
            getClassScopesIncompatibility(expectClassSymbol, actualClass, substitutor, languageVersionSettings)?.let { add(it) }
        }
    }

    private inline fun ExpectActualMatchingContext<*>.computeActualClassFromPotentialActualTypealias(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassLikeSymbol: ClassLikeSymbolMarker,
        onNestedTypealias: () -> Nothing?,
        onErroneousTypealias: () -> Nothing?
    ): RegularClassSymbolMarker? = when (actualClassLikeSymbol) {
        is RegularClassSymbolMarker -> actualClassLikeSymbol
        is TypeAliasSymbolMarker -> if (actualClassLikeSymbol.classId.isNestedClass) {
            onNestedTypealias()
        } else {
            // do not report extra error on erroneous typealias
            actualClassLikeSymbol.expandToRegularClass() ?: onErroneousTypealias()
        }
        else -> error("Incorrect actual classifier for $expectClassSymbol: $actualClassLikeSymbol")
    }

    private fun ExpectActualMatchingContext<*>.areCompatibleSupertypes(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
        substitutor: TypeSubstitutorMarker,
    ): Boolean {
        val expectSupertypes = expectClassSymbol.superTypes.filterNot { it.typeConstructor().isAnyConstructor() }
        val actualType = actualClassSymbol.defaultType
        return expectSupertypes.all { expectSupertype ->
            val expectType = substitutor.safeSubstitute(expectSupertype)
            isSubtypeOf(superType = expectType, subType = actualType) &&
                    !isSubtypeOf(superType = actualType, subType = expectType)
        }
    }

    private fun ExpectActualMatchingContext<*>.getClassScopesIncompatibility(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
        substitutor: TypeSubstitutorMarker,
        languageVersionSettings: LanguageVersionSettings,
    ): ExpectActualIncompatibility<*>? {
        val mismatchedMembers: ArrayList<Pair<DeclarationSymbolMarker, Map<ExpectActualMatchingCompatibility.Mismatch, List<DeclarationSymbolMarker?>>>> =
            ArrayList()
        val incompatibleMembers: ArrayList<MemberIncompatibility<*>> =
            ArrayList()

        val actualMembersByName = actualClassSymbol.collectAllMembers(isActualDeclaration = true).groupBy { nameOf(it) }
        val expectMembers = expectClassSymbol.collectAllMembers(isActualDeclaration = false)
            // private expect constructors are yet allowed KT-68688
            .filterNot { it is CallableSymbolMarker && it !is ConstructorSymbolMarker && it.visibility == Visibilities.Private }
        matchAndCheckExpectMembersAgainstPotentialActuals(
            expectClassSymbol,
            actualClassSymbol,
            substitutor,
            languageVersionSettings,
            expectMembers,
            actualMembersByName,
            outToMismatchedMembers = mismatchedMembers,
            outToIncompatibleMembers = incompatibleMembers
        )

        val actualStaticMembersByName = actualClassSymbol.collectAllStaticCallables(isActualDeclaration = true).groupBy { nameOf(it) }
        val expectStaticMembers = expectClassSymbol.collectAllStaticCallables(isActualDeclaration = false)
        matchAndCheckExpectMembersAgainstPotentialActuals(
            expectClassSymbol,
            actualClassSymbol,
            substitutor,
            languageVersionSettings,
            expectStaticMembers,
            actualStaticMembersByName,
            outToMismatchedMembers = mismatchedMembers,
            outToIncompatibleMembers = incompatibleMembers
        )

        if (expectClassSymbol.classKind == ClassKind.ENUM_CLASS && actualClassSymbol.classKind == ClassKind.ENUM_CLASS) {
            val aEntries = expectClassSymbol.collectEnumEntryNames()
            val bEntries = actualClassSymbol.collectEnumEntryNames()

            if (!bEntries.containsAll(aEntries)) return ExpectActualIncompatibility.EnumEntries
        }

        // TODO: check static scope?

        return when (mismatchedMembers.isNotEmpty() || incompatibleMembers.isNotEmpty()) {
            true -> ExpectActualIncompatibility.ClassScopes(mismatchedMembers, incompatibleMembers)
            false -> null
        }
    }

    private fun ExpectActualMatchingContext<*>.matchAndCheckExpectMembersAgainstPotentialActuals(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
        substitutor: TypeSubstitutorMarker,
        languageVersionSettings: LanguageVersionSettings,
        expectMembers: List<DeclarationSymbolMarker>,
        actualMembersByName: Map<Name, List<DeclarationSymbolMarker>>,
        // out
        outToMismatchedMembers: ArrayList<Pair<DeclarationSymbolMarker, Map<ExpectActualMatchingCompatibility.Mismatch, List<DeclarationSymbolMarker?>>>>,
        outToIncompatibleMembers: ArrayList<MemberIncompatibility<*>>
    ) {
        for (expectMember in expectMembers) {
            val actualMembers = getPossibleActualsByExpectName(expectMember, actualMembersByName)
            val matched = matchSingleExpectAgainstPotentialActuals(
                expectMember,
                actualMembers,
                substitutor,
                expectClassSymbol,
                actualClassSymbol,
                outToMismatchedMembers
            )
            for (it in matched) {
                checkSingleExpectAgainstMatchedActual(
                    expectMember,
                    it,
                    substitutor,
                    expectClassSymbol,
                    actualClassSymbol,
                    outToIncompatibleMembers,
                    languageVersionSettings
                )
            }
        }
    }

    private fun ExpectActualMatchingContext<*>.checkSingleExpectAgainstMatchedActual(
        expectMember: DeclarationSymbolMarker,
        actualMember: DeclarationSymbolMarker,
        substitutor: TypeSubstitutorMarker?,
        expectClassSymbol: RegularClassSymbolMarker?,
        actualClassSymbol: RegularClassSymbolMarker?,
        incompatibleMembers: MutableList<MemberIncompatibility<*>>?,
        languageVersionSettings: LanguageVersionSettings,
    ) {
        val incompatible = when {
            skipCheckingOnExpectActualPair(expectMember, actualMember) -> {
                if (expectMember is RegularClassSymbolMarker && actualMember is ClassLikeSymbolMarker) {
                    computeActualClassFromPotentialActualTypealias(
                        expectMember,
                        actualMember,
                        onNestedTypealias = { null },
                        onErroneousTypealias = { null }
                    )?.let { actualClass ->
                        val parentSubstitutor = substitutor?.takeIf { !innerClassesCapturesOuterTypeParameters }
                        (expectMember.typeParameters zipIfSizesAreEqual actualClass.typeParameters)?.let {
                            val substitutor = createExpectActualTypeParameterSubstitutor(it, parentSubstitutor)

                            // Here we call check for two classes only to match the scopes of these classes, so the return value is ignored.
                            // Abstraction of matching leaked into checking in this place :sad:
                            getClassScopesIncompatibility(expectMember, actualClass, substitutor, languageVersionSettings)
                        }
                    }
                }
                emptyList()
            }

            expectMember is CallableSymbolMarker -> getCallablesCompatibility(
                expectMember,
                actualMember as CallableSymbolMarker,
                substitutor,
                expectClassSymbol,
                actualClassSymbol,
                languageVersionSettings,
            )

            expectMember is RegularClassSymbolMarker -> {
                val parentSubstitutor = substitutor?.takeIf { !innerClassesCapturesOuterTypeParameters }
                getClassifiersCompatibility(
                    expectMember,
                    actualMember as ClassLikeSymbolMarker,
                    parentSubstitutor,
                    languageVersionSettings,
                )
            }
            else -> error("Unsupported declaration: $expectMember ($actualMember)")
        }

        for (member in incompatible) {
            incompatibleMembers?.add(MemberIncompatibility(expectMember, actualMember, member))
            onIncompatibleMembersFromClassScope(expectMember, actualMember, member, expectClassSymbol, actualClassSymbol)
        }
    }

    private fun ExpectActualMatchingContext<*>.getCallablesCompatibility(
        expectDeclaration: CallableSymbolMarker,
        actualDeclaration: CallableSymbolMarker,
        parentSubstitutor: TypeSubstitutorMarker?,
        expectContainingClass: RegularClassSymbolMarker?,
        actualContainingClass: RegularClassSymbolMarker?,
        languageVersionSettings: LanguageVersionSettings,
    ): List<ExpectActualIncompatibility<*>> = buildList {
        checkCallablesInvariants(expectDeclaration, actualDeclaration)

        if (areEnumConstructors(expectDeclaration, actualDeclaration, expectContainingClass, actualContainingClass)) {
            return@getCallablesCompatibility emptyList()
        }

        val insideAnnotationClass = expectContainingClass?.classKind == ClassKind.ANNOTATION_CLASS
        val expectedTypeParameters = expectDeclaration.typeParameters
        val actualTypeParameters = actualDeclaration.typeParameters
        val expectedValueParameters = expectDeclaration.valueParameters
        val actualValueParameters = actualDeclaration.valueParameters
        val expectedContextParameters = expectDeclaration.contextParameters
        val actualContextParameters = actualDeclaration.contextParameters

        val substitutor = (expectedTypeParameters zipIfSizesAreEqual actualTypeParameters)?.let {
            createExpectActualTypeParameterSubstitutor(it, parentSubstitutor)
        }

        if (substitutor != null && !areCompatibleExpectActualTypes(
                substitutor.safeSubstitute(expectDeclaration.returnType),
                actualDeclaration.returnType,
                parameterOfAnnotationComparisonMode = insideAnnotationClass,
                dynamicTypesEqualToAnything = false
            )
        ) {
            add(ExpectActualIncompatibility.ReturnType)
        }

        if (
            actualDeclaration.hasStableParameterNames &&
            expectDeclaration.hasStableParameterNames &&
            sizesAreEqualAndElementsNotEqualBy(expectedValueParameters, actualValueParameters) { nameOf(it) }
        ) {
            add(ExpectActualIncompatibility.ParameterNames)
        }

        if (
            languageVersionSettings.supportsFeature(LanguageFeature.ContextParameters) &&
            actualDeclaration.hasStableParameterNames &&
            expectDeclaration.hasStableParameterNames &&
            sizesAreEqualAndElementsNotEqualBy(expectedContextParameters, actualContextParameters) { nameOf(it) }
        ) {
            add(ExpectActualIncompatibility.ContextParameterNames)
        }

        if (languageVersionSettings.getFlag(AnalysisFlags.returnValueCheckerMode) != ReturnValueCheckerMode.DISABLED) {
            if (mustUseMatcher?.matches(expectDeclaration, actualDeclaration, expectContainingClass) == false) {
                add(ExpectActualIncompatibility.IgnorabilityIsDifferent)
            }
        }

        if (sizesAreEqualAndElementsNotEqualBy(expectedTypeParameters, actualTypeParameters) { nameOf(it) }) {
            add(ExpectActualIncompatibility.TypeParameterNames)
        }

        val expectModality = expectDeclaration.modality
        val actualModality = actualDeclaration.modality
        if (
            !areCompatibleModalities(
                expectModality,
                actualModality,
                expectContainingClass?.modality,
                actualContainingClass?.modality
            )
        ) {
            add(ExpectActualIncompatibility.Modality(expectModality, actualModality))
        }

        if (!areCompatibleCallableVisibilities(
                expectDeclaration.visibility,
                expectModality,
                expectContainingClass?.modality,
                actualDeclaration.visibility,
                languageVersionSettings
            )
        ) {
            add(ExpectActualIncompatibility.Visibility)
        }

        getTypeParametersVarianceOrReifiedIncompatibility(expectedTypeParameters, actualTypeParameters)?.let { add(it) }

        if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitDefaultArgumentsInExpectActualizedByFakeOverride) &&
            // If expect declaration is a fake-override then default params came from common
            // supertypes of actual class and expect class. It's a valid code.
            !expectDeclaration.isFakeOverride(expectContainingClass) &&
            (actualDeclaration.isFakeOverride(actualContainingClass) || actualDeclaration.isDelegatedMember) &&
            expectDeclaration.valueParameters.any { it.hasDefaultValueNonRecursive }
        ) {
            add(ExpectActualIncompatibility.ParametersWithDefaultValuesInExpectActualizedByFakeOverride)
        }

        if (shouldCheckDefaultParams &&
            // "parameters" checks are required only for functions, because only functions can have parameters
            actualDeclaration is FunctionSymbolMarker && expectDeclaration is FunctionSymbolMarker &&
            // Actual annotation constructors can have default argument values; their consistency with arguments in the expected annotation
            // is checked in ExpectedActualDeclarationChecker.checkAnnotationConstructors
            !actualDeclaration.isAnnotationConstructor()
        ) {
            val expectOverriddenDeclarations =
                expectDeclaration.allRecursivelyOverriddenDeclarationsIncludingSelf(expectContainingClass).toSet()
            val actualOverriddenDeclarations =
                actualDeclaration.allRecursivelyOverriddenDeclarationsIncludingSelf(actualContainingClass)

            // If default params came from common supertypes of actual class and expect class then it's a valid code.
            // Here we filter out such default params.
            if ((actualOverriddenDeclarations - expectOverriddenDeclarations).flatMap { it.valueParameters }.any { it.hasDefaultValue }) {
                add(ExpectActualIncompatibility.ActualFunctionWithOptionalParameters)
            }
        }

        if (sizesAreEqualAndElementsNotEqualBy(expectedValueParameters, actualValueParameters) { it.isVararg }) {
            add(ExpectActualIncompatibility.ValueParameterVararg)
        }

        // Adding noinline/crossinline to parameters is disallowed, except if the expected declaration was not inline at all
        if (expectDeclaration is SimpleFunctionSymbolMarker && expectDeclaration.isInline) {
            if (expectedValueParameters.indices.any { i -> !expectedValueParameters[i].isNoinline && actualValueParameters[i].isNoinline }) {
                add(ExpectActualIncompatibility.ValueParameterNoinline)
            }
            if (expectedValueParameters.indices.any { i -> !expectedValueParameters[i].isCrossinline && actualValueParameters[i].isCrossinline }) {
                add(ExpectActualIncompatibility.ValueParameterCrossinline)
            }
        }

        when {
            expectDeclaration is FunctionSymbolMarker && actualDeclaration is FunctionSymbolMarker ->
                getFunctionsIncompatibility(expectDeclaration, actualDeclaration)?.let { add(it) }

            expectDeclaration is PropertySymbolMarker && actualDeclaration is PropertySymbolMarker ->
                getPropertiesIncompatibility(expectDeclaration, actualDeclaration, expectContainingClass, languageVersionSettings)?.let { add(it) }

            expectDeclaration is EnumEntrySymbolMarker && actualDeclaration is EnumEntrySymbolMarker -> {
                // do nothing, entries are matched only by name
            }

            actualDeclaration.isJavaField && expectDeclaration.canBeActualizedByJavaField -> {
                // no specific checks, actualization by Java field is permitted in a limited well-known number of cases
            }

            else -> error("Unsupported declarations: $expectDeclaration, $actualDeclaration")
        }
    }

    private fun ExpectActualMatchingContext<*>.areCompatibleClassKinds(
        expectClass: RegularClassSymbolMarker,
        actualClass: RegularClassSymbolMarker,
    ): Boolean {
        if (expectClass.classKind == actualClass.classKind) return true

        if (expectClass.classKind == ClassKind.CLASS && isFinal(expectClass) && isCtorless(expectClass)) {
            if (actualClass.classKind == ClassKind.OBJECT) return true
        }

        return false
    }

    private fun areCompatibleModalities(
        expectModality: Modality?,
        actualModality: Modality?,
        expectContainingClassModality: Modality? = null,
        actualContainingClassModality: Modality? = null,
    ): Boolean {
        val expectEffectiveModality = effectiveModality(expectModality, expectContainingClassModality)
        val actualEffectiveModality = effectiveModality(actualModality, actualContainingClassModality)

        return actualEffectiveModality in compatibleModalityMap.getValue(expectEffectiveModality)
    }

    /*
     * If containing class is final then all declarations in it effectively final
     */
    private fun effectiveModality(declarationModality: Modality?, containingClassModality: Modality?): Modality? {
        return when (containingClassModality) {
            Modality.FINAL -> Modality.FINAL
            else -> declarationModality
        }
    }

    /*
     * Key is expect modality, value is a set of compatible actual modalities
     */
    private val compatibleModalityMap: EnumMap<Modality, EnumSet<Modality>> = enumMapOf(
        Modality.ABSTRACT to enumSetOf(Modality.ABSTRACT),
        Modality.OPEN to enumSetOf(Modality.OPEN),
        Modality.FINAL to enumSetOf(Modality.OPEN, Modality.FINAL),
        Modality.SEALED to enumSetOf(Modality.SEALED),
    )

    private fun areCompatibleCallableVisibilities(
        expectVisibility: Visibility,
        expectModality: Modality?,
        expectContainingClassModality: Modality?,
        actualVisibility: Visibility,
        languageVersionSettings: LanguageVersionSettings
    ): Boolean {
        // In the case of actualization by a Java declaration such as a field or a method normalize the Java visibility
        // to the closest Kotlin visibility.Example: "protected_and_package" -> "protected".
        val normalizedExpectVisibility = expectVisibility.normalize()
        val normalizedActualVisibility = actualVisibility.normalize()

        val compare = Visibilities.compare(normalizedExpectVisibility, normalizedActualVisibility)

        val effectiveModality =
            when (languageVersionSettings.supportsFeature(LanguageFeature.SupportEffectivelyFinalInExpectActualVisibilityCheck)) {
                true -> effectiveModality(expectModality, expectContainingClassModality)
                false -> expectModality
            }
        return if (effectiveModality != Modality.FINAL) {
            // For overridable declarations visibility should match precisely, see KT-19664
            compare == 0
        } else {
            // For non-overridable declarations actuals are allowed to have more permissive visibility
            compare != null && compare <= 0
        }
    }

    private fun ExpectActualMatchingContext<*>.areCompatibleClassVisibilities(
        expectClassSymbol: RegularClassSymbolMarker,
        actualClassSymbol: RegularClassSymbolMarker,
    ): Boolean {
        val expectVisibility = expectClassSymbol.visibility
        val actualVisibility = actualClassSymbol.visibility
        if (expectVisibility == actualVisibility) return true
        val result = Visibilities.compare(actualVisibility, expectVisibility)
        return result != null && result > 0
    }

    private fun ExpectActualMatchingContext<*>.getTypeParametersVarianceOrReifiedIncompatibility(
        expectTypeParameterSymbols: List<TypeParameterSymbolMarker>,
        actualTypeParameterSymbols: List<TypeParameterSymbolMarker>,
    ): ExpectActualIncompatibility<*>? {
        if (sizesAreEqualAndElementsNotEqualBy(expectTypeParameterSymbols, actualTypeParameterSymbols) { it.variance }) {
            return ExpectActualIncompatibility.TypeParameterVariance
        }

        // Removing "reified" from an expected function's type parameter is fine
        if ((expectTypeParameterSymbols zipIfSizesAreEqual actualTypeParameterSymbols)?.any { (e, a) -> !e.isReified && a.isReified } == true) {
            return ExpectActualIncompatibility.TypeParameterReified
        }

        return null
    }

    private fun ExpectActualMatchingContext<*>.getFunctionsIncompatibility(
        expectFunction: CallableSymbolMarker,
        actualFunction: CallableSymbolMarker,
    ): ExpectActualIncompatibility<*>? {
        if (!equalBy(expectFunction, actualFunction) { f -> f.isSuspend }) {
            return ExpectActualIncompatibility.FunctionModifiersDifferent
        }

        if (
            expectFunction.isInfix && !actualFunction.isInfix ||
            expectFunction.isInline && !actualFunction.isInline ||
            expectFunction.isOperator && !actualFunction.isOperator
        ) {
            return ExpectActualIncompatibility.FunctionModifiersNotSubset
        }

        return null
    }

    private fun ExpectActualMatchingContext<*>.getPropertiesIncompatibility(
        expected: PropertySymbolMarker,
        actual: PropertySymbolMarker,
        expectContainingClass: RegularClassSymbolMarker?,
        languageVersionSettings: LanguageVersionSettings,
    ): ExpectActualIncompatibility<*>? {
        return when {
            !equalBy(expected, actual) { p -> p.isVar } -> ExpectActualIncompatibility.PropertyKind
            !equalBy(expected, actual) { p -> p.isLateinit } -> ExpectActualIncompatibility.PropertyLateinitModifier
            expected.isConst && !actual.isConst -> ExpectActualIncompatibility.PropertyConstModifier
            !arePropertySettersWithCompatibleVisibilities(expected, actual, expectContainingClass, languageVersionSettings) ->
                ExpectActualIncompatibility.PropertySetterVisibility
            else -> null
        }
    }

    private fun ExpectActualMatchingContext<*>.arePropertySettersWithCompatibleVisibilities(
        expected: PropertySymbolMarker,
        actual: PropertySymbolMarker,
        expectContainingClass: RegularClassSymbolMarker?,
        languageVersionSettings: LanguageVersionSettings,
    ): Boolean {
        val expectedSetter = expected.setter ?: return true
        val actualSetter = actual.setter ?: return true
        return areCompatibleCallableVisibilities(
            expectedSetter.visibility,
            expectedSetter.modality,
            expectContainingClass?.modality,
            actualSetter.visibility,
            languageVersionSettings,
        )
    }

    // ---------------------------------------- Utils ----------------------------------------

    internal inline fun <T, K> sizesAreEqualAndElementsNotEqualBy(first: List<T>, second: List<T>, selector: (T) -> K): Boolean {
        if (first.size != second.size) return false
        for (i in first.indices) {
            if (selector(first[i]) != selector(second[i])) return true
        }
        return false
    }

    private inline fun <T, K> equalBy(first: T, second: T, selector: (T) -> K): Boolean =
        selector(first) == selector(second)

    private fun ExpectActualMatchingContext<*>.isCtorless(regularClass: RegularClassSymbolMarker): Boolean {
        return regularClass.getCallablesForExpectClass(SpecialNames.INIT).isEmpty()
    }

    private fun ExpectActualMatchingContext<*>.isFinal(regularClassSymbolMarker: RegularClassSymbolMarker): Boolean {
        return regularClassSymbolMarker.modality == Modality.FINAL
    }
}

fun ExpectActualMatchingContext<*>.isIllegalRequiresOptInAnnotation(
    on: RegularClassSymbolMarker, // actual or expect
    expect: RegularClassSymbolMarker,
    languageVersionSettings: LanguageVersionSettings,
): Boolean {
    return languageVersionSettings.supportsFeature(LanguageFeature.MultiplatformRestrictions) &&
            on.classKind.isAnnotationClass &&
            expect.annotations.none { it.classId == StandardClassIds.Annotations.OptionalExpectation } &&
            on.annotations.any { it.classId == OptInNames.REQUIRES_OPT_IN_CLASS_ID }
}
