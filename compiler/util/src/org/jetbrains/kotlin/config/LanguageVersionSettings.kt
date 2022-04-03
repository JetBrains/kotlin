/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.config.LanguageFeature.Kind.*
import org.jetbrains.kotlin.config.LanguageVersion.*
import org.jetbrains.kotlin.utils.DescriptionAware
import java.util.*

enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.KOTLIN_1_0,
    val hintUrl: String? = null,
    val defaultState: State = State.ENABLED,
    val kind: Kind = OTHER // NB: default value OTHER doesn't force pre-releaseness (see KDoc)
) {
    // Note: names of these entries are also used in diagnostic tests and in user-visible messages (see presentableText below)

    // 1.1

    TypeAliases(KOTLIN_1_1),
    BoundCallableReferences(KOTLIN_1_1, ApiVersion.KOTLIN_1_1),
    LocalDelegatedProperties(KOTLIN_1_1, ApiVersion.KOTLIN_1_1),
    TopLevelSealedInheritance(KOTLIN_1_1),
    AdditionalBuiltInsMembers(KOTLIN_1_1),
    DataClassInheritance(KOTLIN_1_1),
    InlineProperties(KOTLIN_1_1),
    DestructuringLambdaParameters(KOTLIN_1_1),
    SingleUnderscoreForParameterName(KOTLIN_1_1),
    DslMarkersSupport(KOTLIN_1_1),
    UnderscoresInNumericLiterals(KOTLIN_1_1),
    DivisionByZeroInConstantExpressions(KOTLIN_1_1),
    InlineConstVals(KOTLIN_1_1),
    OperatorRem(KOTLIN_1_1),
    OperatorProvideDelegate(KOTLIN_1_1),
    ShortSyntaxForPropertyGetters(KOTLIN_1_1),
    RefinedSamAdaptersPriority(KOTLIN_1_1),
    SafeCallBoundSmartCasts(KOTLIN_1_1),
    TypeInferenceOnGenericsForCallableReferences(KOTLIN_1_1),
    NoDelegationToJavaDefaultInterfaceMembers(KOTLIN_1_1),
    DefaultImportOfPackageKotlinComparisons(KOTLIN_1_1),

    // 1.2

    InlineDefaultFunctionalParameters(KOTLIN_1_2),
    SoundSmartCastsAfterTry(KOTLIN_1_2),
    DeprecatedFieldForInvisibleCompanionObject(KOTLIN_1_2),
    NullabilityAssertionOnExtensionReceiver(KOTLIN_1_2),
    SafeCastCheckBoundSmartCasts(KOTLIN_1_2),
    CapturedInClosureSmartCasts(KOTLIN_1_2),
    LateinitTopLevelProperties(KOTLIN_1_2),
    LateinitLocalVariables(KOTLIN_1_2),
    InnerClassInEnumEntryClass(KOTLIN_1_2),
    CallableReferencesToClassMembersWithEmptyLHS(KOTLIN_1_2),
    ThrowNpeOnExplicitEqualsForBoxedNull(KOTLIN_1_2),
    JvmPackageName(KOTLIN_1_2),
    AssigningArraysToVarargsInNamedFormInAnnotations(KOTLIN_1_2),
    ExpectedTypeFromCast(KOTLIN_1_2),
    DefaultMethodsCallFromJava6TargetError(KOTLIN_1_2),

    // 1.3

    RestrictionOfValReassignmentViaBackingField(KOTLIN_1_3, kind = BUG_FIX),
    NestedClassesInEnumEntryShouldBeInner(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitDataClassesOverridingCopy(KOTLIN_1_3, kind = BUG_FIX),
    RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitInnerClassesOfGenericClassExtendingThrowable(KOTLIN_1_3, kind = BUG_FIX),
    ProperForInArrayLoopRangeVariableAssignmentSemantic(KOTLIN_1_3, kind = BUG_FIX),
    NestedClassesInAnnotations(KOTLIN_1_3),
    JvmStaticInInterface(KOTLIN_1_3, kind = UNSTABLE_FEATURE),
    JvmFieldInInterface(KOTLIN_1_3, kind = UNSTABLE_FEATURE),
    ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitNonConstValuesAsVarargsInAnnotations(KOTLIN_1_3, kind = BUG_FIX),
    ReleaseCoroutines(KOTLIN_1_3, kind = UNSTABLE_FEATURE),
    ReadDeserializedContracts(KOTLIN_1_3),
    UseReturnsEffect(KOTLIN_1_3),
    UseCallsInPlaceEffect(KOTLIN_1_3),
    AllowContractsForCustomFunctions(KOTLIN_1_3),
    VariableDeclarationInWhenSubject(KOTLIN_1_3),
    ProhibitLocalAnnotations(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitSmartcastsOnLocalDelegatedProperty(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitOperatorMod(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitAssigningSingleElementsToVarargsInNamedForm(KOTLIN_1_3, kind = BUG_FIX),
    FunctionTypesWithBigArity(KOTLIN_1_3, sinceApiVersion = ApiVersion.KOTLIN_1_3),
    RestrictRetentionForExpressionAnnotations(KOTLIN_1_3, kind = BUG_FIX),
    NormalizeConstructorCalls(KOTLIN_1_3),
    StrictJavaNullabilityAssertions(KOTLIN_1_3, kind = BUG_FIX),
    SoundSmartcastForEnumEntries(KOTLIN_1_3, kind = BUG_FIX),
    ProhibitErroneousExpressionsInAnnotationsWithUseSiteTargets(KOTLIN_1_3, kind = BUG_FIX),
    NewCapturedReceiverFieldNamingConvention(KOTLIN_1_3, kind = BUG_FIX),
    ExtendedMainConvention(KOTLIN_1_3),
    ExperimentalBuilderInference(KOTLIN_1_3),

    // 1.4

    DslMarkerOnFunctionTypeReceiver(KOTLIN_1_4, kind = BUG_FIX),
    RestrictReturnStatementTarget(KOTLIN_1_4, kind = BUG_FIX),
    NoConstantValueAttributeForNonConstVals(KOTLIN_1_4, kind = BUG_FIX),
    WarningOnMainUnusedParameter(KOTLIN_1_4),
    PolymorphicSignature(KOTLIN_1_4),
    ProhibitConcurrentHashMapContains(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitTypeParametersForLocalVariables(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitTypeParametersInAnonymousObjects(KOTLIN_1_4, kind = BUG_FIX),
    ProperInlineFromHigherPlatformDiagnostic(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitRepeatedUseSiteTargetAnnotations(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitUseSiteTargetAnnotationsOnSuperTypes(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitTypeParametersInClassLiteralsInAnnotationArguments(KOTLIN_1_4, kind = BUG_FIX),
    ProhibitComparisonOfIncompatibleEnums(KOTLIN_1_4, kind = BUG_FIX),
    BareArrayClassLiteral(KOTLIN_1_4),
    ProhibitGenericArrayClassLiteral(KOTLIN_1_4),
    NonParenthesizedAnnotationsOnFunctionalTypes(KOTLIN_1_4),
    UseGetterNameForPropertyAnnotationsMethodOnJvm(KOTLIN_1_4),
    AllowBreakAndContinueInsideWhen(KOTLIN_1_4),
    MixedNamedArgumentsInTheirOwnPosition(KOTLIN_1_4),
    ProhibitTailrecOnVirtualMember(KOTLIN_1_4, kind = BUG_FIX),
    ProperComputationOrderOfTailrecDefaultParameters(KOTLIN_1_4),
    TrailingCommas(KOTLIN_1_4),
    ProhibitProtectedCallFromInline(KOTLIN_1_4, kind = BUG_FIX),
    ProperFinally(KOTLIN_1_4, kind = BUG_FIX),
    AllowAssigningArrayElementsToVarargsInNamedFormForFunctions(KOTLIN_1_4),
    AllowNullOperatorsForResult(KOTLIN_1_4),
    AllowResultInReturnType(KOTLIN_1_4, defaultState = State.DISABLED),
    PreferJavaFieldOverload(KOTLIN_1_4),
    AllowContractsForNonOverridableMembers(KOTLIN_1_4),
    AllowReifiedGenericsInContracts(KOTLIN_1_4),
    ProperVisibilityForCompanionObjectInstanceField(KOTLIN_1_4, kind = BUG_FIX),
    DoNotGenerateThrowsForDelegatedKotlinMembers(KOTLIN_1_4),
    ProperIeee754Comparisons(KOTLIN_1_4, kind = BUG_FIX),
    FunctionalInterfaceConversion(KOTLIN_1_4, kind = UNSTABLE_FEATURE),
    GenerateJvmOverloadsAsFinal(KOTLIN_1_4),
    MangleClassMembersReturningInlineClasses(KOTLIN_1_4),
    ImproveReportingDiagnosticsOnProtectedMembersOfBaseClass(KOTLIN_1_4, kind = BUG_FIX, defaultState = State.ENABLED),

    NewInference(KOTLIN_1_4),

    // In the next block, features can be enabled only along with new inference
    // v----------------------------------------------------------------------v
    SamConversionForKotlinFunctions(KOTLIN_1_4),
    SamConversionPerArgument(KOTLIN_1_4),
    FunctionReferenceWithDefaultValueAsOtherType(KOTLIN_1_4),
    UnitConversion(KOTLIN_1_4, defaultState = State.DISABLED),
    OverloadResolutionByLambdaReturnType(KOTLIN_1_4),
    ContractsOnCallsWithImplicitReceiver(KOTLIN_1_4),
    BooleanElvisBoundSmartCasts(KOTLIN_1_3, defaultState = State.DISABLED), // see KT-26357 for details
    NewDataFlowForTryExpressions(KOTLIN_1_4, defaultState = State.DISABLED),
    ReferencesToSyntheticJavaProperties(KOTLIN_1_3, defaultState = State.DISABLED),
    // ^----------------------------------------------------------------------^

    // 1.5

    ProhibitSpreadOnSignaturePolymorphicCall(KOTLIN_1_5, kind = BUG_FIX),
    ProhibitInvisibleAbstractMethodsInSuperclasses(KOTLIN_1_5, kind = BUG_FIX),
    ProhibitNonReifiedArraysAsReifiedTypeArguments(KOTLIN_1_5, kind = BUG_FIX),
    ProhibitVarargAsArrayAfterSamArgument(KOTLIN_1_5, kind = BUG_FIX),
    CorrectSourceMappingSyntax(KOTLIN_1_5, kind = UNSTABLE_FEATURE),
    ProperArrayConventionSetterWithDefaultCalls(KOTLIN_1_5, kind = OTHER),
    DisableCompatibilityModeForNewInference(KOTLIN_1_5, defaultState = State.DISABLED),
    AdaptedCallableReferenceAgainstReflectiveType(KOTLIN_1_5, defaultState = State.DISABLED),
    InferenceCompatibility(KOTLIN_1_5, kind = BUG_FIX),
    RequiredPrimaryConstructorDelegationCallInEnums(KOTLIN_1_5, kind = BUG_FIX),
    ApproximateAnonymousReturnTypesInPrivateInlineFunctions(KOTLIN_1_5, kind = BUG_FIX),
    ForbidReferencingToUnderscoreNamedParameterOfCatchBlock(KOTLIN_1_5, kind = BUG_FIX),
    UseCorrectExecutionOrderForVarargArguments(KOTLIN_1_5, kind = BUG_FIX),
    JvmRecordSupport(KOTLIN_1_5),
    AllowNullOperatorsForResultAndResultReturnTypeByDefault(KOTLIN_1_5, defaultState = State.ENABLED),
    AllowSealedInheritorsInDifferentFilesOfSamePackage(KOTLIN_1_5),
    SealedInterfaces(KOTLIN_1_5),
    JvmIrEnabledByDefault(KOTLIN_1_5),
    JvmInlineValueClasses(KOTLIN_1_5, defaultState = State.ENABLED, kind = OTHER),
    SuspendFunctionsInFunInterfaces(KOTLIN_1_5, defaultState = State.ENABLED, kind = OTHER),
    SamWrapperClassesAreSynthetic(KOTLIN_1_5, defaultState = State.ENABLED, kind = BUG_FIX),
    StrictOnlyInputTypesChecks(KOTLIN_1_5),

    JsEnableExtensionFunctionInExternals(KOTLIN_1_5, defaultState = State.DISABLED, kind = OTHER),

    // Disabled until the breaking change is approved by the committee, see KT-10884.
    PackagePrivateFileClassesWithAllPrivateMembers(KOTLIN_1_5, defaultState = State.DISABLED),

    // 1.6

    ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor(KOTLIN_1_6, kind = BUG_FIX),
    PrivateInFileEffectiveVisibility(KOTLIN_1_6, kind = BUG_FIX),
    ProhibitSelfCallsInNestedObjects(KOTLIN_1_6, kind = BUG_FIX),
    ProperCheckAnnotationsTargetInTypeUsePositions(KOTLIN_1_6, kind = BUG_FIX),

    SuspendFunctionAsSupertype(KOTLIN_1_6),
    UnrestrictedBuilderInference(KOTLIN_1_6),
    ClassTypeParameterAnnotations(KOTLIN_1_6),
    TypeInferenceOnCallsWithSelfTypes(KOTLIN_1_6),
    WarnAboutNonExhaustiveWhenOnAlgebraicTypes(KOTLIN_1_6, kind = BUG_FIX),
    InstantiationOfAnnotationClasses(KOTLIN_1_6),
    OptInContagiousSignatures(KOTLIN_1_6, kind = BUG_FIX),
    RepeatableAnnotations(KOTLIN_1_6),
    RepeatableAnnotationContainerConstraints(KOTLIN_1_6, kind = BUG_FIX),
    UseBuilderInferenceOnlyIfNeeded(KOTLIN_1_6),
    SuspendConversion(KOTLIN_1_6),
    ProhibitSuperCallsFromPublicInline(KOTLIN_1_6),
    ProhibitProtectedConstructorCallFromPublicInline(KOTLIN_1_6),

    // 1.7

    /*
     * Improvements include the following:
     *  - taking into account for type enhancement freshly supported type use annotations: KT-11454
     *  - use annotations in the type parameter position to enhance corresponding types: KT-11454
     *  - proper support of the type enhancement of the annotated java arrays: KT-24392
     *  - proper support of the type enhancement of the annotated java varargs' elements: KT-18768
     *  - type enhancement based on annotated bounds of type parameters
     *  - type enhancement within type arguments of the base classes and interfaces
     *  - support type enhancement based on type use annotations on java fields
     *  - preference of a type use annotation to annotation of another type: KT-24392
     *      (if @NotNull has TYPE_USE and METHOD target, then `@NotNull Integer []` -> `Array<Int>..Array<out Int>?` instead of `Array<Int>..Array<out Int>`)
     */
    TypeEnhancementImprovementsInStrictMode(KOTLIN_1_7),
    OptInRelease(KOTLIN_1_7),
    ProhibitNonExhaustiveWhenOnAlgebraicTypes(KOTLIN_1_7, kind = BUG_FIX),
    UseBuilderInferenceWithoutAnnotation(KOTLIN_1_7),
    ProhibitSmartcastsOnPropertyFromAlienBaseClass(KOTLIN_1_7, kind = BUG_FIX),
    ProhibitInvalidCharsInNativeIdentifiers(KOTLIN_1_7, kind = BUG_FIX),
    DefinitelyNonNullableTypes(KOTLIN_1_7),
    ReportNonVarargSpreadOnGenericCalls(KOTLIN_1_7, kind = BUG_FIX),
    ProhibitSimplificationOfNonTrivialConstBooleanExpressions(KOTLIN_1_7),
    SafeCallsAreAlwaysNullable(KOTLIN_1_7),
    RefineTypeCheckingOnAssignmentsToJavaFields(KOTLIN_1_7),
    JvmPermittedSubclassesAttributeForSealed(KOTLIN_1_7),
    ProperTypeInferenceConstraintsProcessing(KOTLIN_1_7, kind = BUG_FIX),
    ForbidExposingTypesInPrimaryConstructorProperties(KOTLIN_1_7, kind = BUG_FIX),
    PartiallySpecifiedTypeArguments(KOTLIN_1_7),
    EliminateAmbiguitiesWithExternalTypeParameters(KOTLIN_1_7),
    EliminateAmbiguitiesOnInheritedSamInterfaces(KOTLIN_1_7),
    ConsiderExtensionReceiverFromConstrainsInLambda(KOTLIN_1_7, kind = BUG_FIX), // KT-49832
    ProperInternalVisibilityCheckInImportingScope(KOTLIN_1_7, kind = BUG_FIX),
    InlineClassImplementationByDelegation(KOTLIN_1_7),
    QualifiedSupertypeMayBeExtendedByOtherSupertype(KOTLIN_1_7),
    YieldIsNoMoreReserved(KOTLIN_1_7),
    NoDeprecationOnDeprecatedEnumEntries(KOTLIN_1_7), // KT-37975
    ProhibitQualifiedAccessToUninitializedEnumEntry(KOTLIN_1_7, kind = BUG_FIX), // KT-41124
    ForbidRecursiveDelegateExpressions(KOTLIN_1_7, kind = BUG_FIX),
    KotlinFunInterfaceConstructorReference(KOTLIN_1_7),
    SuspendOnlySamConversions(KOTLIN_1_7),

    // 1.8

    DontLoseDiagnosticsDuringOverloadResolutionByReturnType(KOTLIN_1_8),
    ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated(KOTLIN_1_8, kind = BUG_FIX), // KT-36770
    ProhibitConfusingSyntaxInWhenBranches(KOTLIN_1_8, kind = BUG_FIX), // KT-48385
    UseConsistentRulesForPrivateConstructorsOfSealedClasses(sinceVersion = KOTLIN_1_8, kind = BUG_FIX), // KT-44866
    ProgressionsChangingResolve(KOTLIN_1_8), // KT-49276
    AbstractClassMemberNotImplementedWithIntermediateAbstractClass(KOTLIN_1_8, kind = BUG_FIX), // KT-45508
    ForbidSuperDelegationToAbstractFakeOverride(KOTLIN_1_8), // KT-49017
    ForbidSuperDelegationToAbstractAnyMethod(KOTLIN_1_8), // KT-38078
    ProperEqualityChecksInBuilderInferenceCalls(KOTLIN_1_8, kind = BUG_FIX),
    ProhibitNonExhaustiveIfInRhsOfElvis(KOTLIN_1_8, kind = BUG_FIX), // KT-44705
    ProhibitAccessToEnumCompanionMembersInEnumConstructorCall(KOTLIN_1_8, kind = BUG_FIX), // KT-49110
    ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes(KOTLIN_1_8, kind = BUG_FIX), // KT-29168
    IgnoreNullabilityForErasedValueParameters(KOTLIN_1_8, kind = BUG_FIX),
    ForbidUsingExtensionPropertyTypeParameterInDelegate(KOTLIN_1_8, kind = BUG_FIX),
    ModifierNonBuiltinSuspendFunError(KOTLIN_1_8),
    SynchronizedSuspendError(KOTLIN_1_8),

    // 1.9

    DisableCheckingChangedProgressionsResolve(KOTLIN_1_9), // KT-49276
    ProhibitIllegalValueParameterUsageInDefaultArguments(KOTLIN_1_9, kind = BUG_FIX), // KT-25694
    ProhibitConstructorCallOnFunctionalSupertype(KOTLIN_1_9, kind = BUG_FIX), // KT-46344
    ProhibitArrayLiteralsInCompanionOfAnnotation(KOTLIN_1_9, kind = BUG_FIX), // KT-39041
    ProhibitCyclesInAnnotations(KOTLIN_1_9, kind = BUG_FIX), // KT-47932
    ForbidExtensionFunctionTypeOnNonFunctionTypes(KOTLIN_1_9), // related to KT-43527
    ProhibitEnumDeclaringClass(KOTLIN_1_9, kind = BUG_FIX), // KT-49653
    StopPropagatingDeprecationThroughOverrides(KOTLIN_1_9, kind = BUG_FIX), // KT-47902
    ReportTypeVarianceConflictOnQualifierArguments(KOTLIN_1_9, kind = BUG_FIX), // KT-50947
    ReportErrorsOnRecursiveTypeInsidePlusAssignment(KOTLIN_1_9, kind = BUG_FIX), // KT-48546

    // Disabled for indefinite time. See KT-48535 and related discussion
    ApproximateIntegerLiteralTypesInReceiverPosition(sinceVersion = null),

    // Experimental features

    Coroutines(
        KOTLIN_1_1, ApiVersion.KOTLIN_1_1,
        "https://kotlinlang.org/docs/diagnostics/experimental-coroutines",
        State.ENABLED_WITH_WARNING
    ),
    MultiPlatformProjects(sinceVersion = null, defaultState = State.DISABLED),
    InlineClasses(KOTLIN_1_3, defaultState = State.ENABLED_WITH_WARNING, kind = UNSTABLE_FEATURE),
    ProhibitComparisonOfIncompatibleClasses(sinceVersion = null, kind = BUG_FIX, defaultState = State.DISABLED),
    ExplicitBackingFields(sinceVersion = null, defaultState = State.DISABLED, kind = UNSTABLE_FEATURE),
    FunctionalTypeWithExtensionAsSupertype(sinceVersion = KOTLIN_1_6, defaultState = State.DISABLED),
    JsAllowInvalidCharsIdentifiersEscaping(sinceVersion = null, defaultState = State.DISABLED, kind = UNSTABLE_FEATURE),
    JsAllowValueClassesInExternals(sinceVersion = null, defaultState = State.DISABLED, kind = UNSTABLE_FEATURE),
    ContextReceivers(sinceVersion = null, defaultState = State.DISABLED, kind = UNSTABLE_FEATURE),
    GenericInlineClassParameter(sinceVersion = null, defaultState = State.ENABLED_WITH_WARNING, kind = UNSTABLE_FEATURE),
    ValueClasses(sinceVersion = null, defaultState = State.DISABLED, kind = UNSTABLE_FEATURE),
    JavaSamConversionEqualsHashCode(sinceVersion = null, defaultState = State.DISABLED, kind = UNSTABLE_FEATURE),
    ;

    val presentableName: String
        // E.g. "DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::lowercase)

    val presentableText get() = if (hintUrl == null) presentableName else "$presentableName (See: $hintUrl)"

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        ENABLED_WITH_WARNING("Enabled with warning"),
        ENABLED_WITH_ERROR("Disabled"), // TODO: consider dropping this and using DISABLED instead
        DISABLED("Disabled");
    }

    /**
     * # [forcesPreReleaseBinaries]
     * If 'true', then enabling this feature (e.g. by '-XXLanguage:', or dedicated '-X'-flag)
     * will force generation of pre-release binaries (given that [sinceVersion] > [LanguageVersion.LATEST_STABLE]).
     * Use it for features that involve generation of non-trivial low-level code with non-finalized design.
     *
     * Note that [forcesPreReleaseBinaries] makes sense only for features with [sinceVersion] > [LanguageVersion.LATEST_STABLE].
     *
     * Please, DO NOT use features that force pre-release binaries in the Kotlin project, as that would
     * generate 'kotlin-compiler' as pre-release.
     *
     *
     * # [enabledInProgressiveMode]
     * If 'true', then this feature will be automatically enabled under '-progressive' mode.
     *
     * Restrictions for using this flag for particular feature follow from restrictions of the progressive mode:
     * - enabling it *must not* break compatibility with non-progressive compiler, i.e. code written under progressive
     *   should compile successfully by non-progressive compiler with the same language version settings.
     *   Example: making some "red" code "green" is not fine, because non-progressive compilers won't be able to compile
     *   such code
     *
     * - changes in language semantics should not be "silent": user must receive some message from the compiler
     *   about all affected code. Exceptions are possible on case-by-case basis.
     *   Example: silently changing semantics of generated low-level code is not fine, but deprecating some language
     *   construction immediately instead of a going through complete deprecation cycle is fine.
     *
     * NB: Currently, [enabledInProgressiveMode] makes sense only for features with [sinceVersion] > [LanguageVersion.LATEST_STABLE]
     */
    enum class Kind(val enabledInProgressiveMode: Boolean, val forcesPreReleaseBinaries: Boolean) {
        /**
         * Simple bug fix which just forbids some language constructions.
         * Rule of thumb: it turns "green code" into "red".
         *
         * Note that, some actual bug fixes can affect overload resolution/inference, silently changing semantics of
         * users' code -- DO NOT use Kind.BUG_FIX for them!
         */
        BUG_FIX(true, false),

        /**
         * Enables support of some new and *unstable* construction in language.
         * Rule of thumb: it turns "red" code into "green", and we want to strongly demotivate people from manually enabling
         * that feature in production.
         */
        UNSTABLE_FEATURE(false, true),

        /**
         * A new feature in the language which has no impact on the binary output of the compiler, and therefore
         * does not cause pre-release binaries to be generated.
         * Rule of thumb: it turns "red" code into "green" and the old compilers can correctly use the binaries
         * produced by the new compiler.
         *
         * NB. OTHER is not a conservative fallback, as it doesn't imply generation of pre-release binaries
         */
        OTHER(false, false),
    }

    companion object {
        @JvmStatic
        fun fromString(str: String) = values().find { it.name == str }
    }
}

enum class LanguageVersion(val major: Int, val minor: Int) : DescriptionAware, LanguageOrApiVersion {
    KOTLIN_1_0(1, 0),
    KOTLIN_1_1(1, 1),
    KOTLIN_1_2(1, 2),
    KOTLIN_1_3(1, 3),
    KOTLIN_1_4(1, 4),
    KOTLIN_1_5(1, 5),
    KOTLIN_1_6(1, 6),
    KOTLIN_1_7(1, 7),
    KOTLIN_1_8(1, 8),
    KOTLIN_1_9(1, 9),
    ;

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override val versionString: String
        get() = "$major.$minor"

    override fun toString() = versionString

    companion object {
        @JvmStatic
        fun fromVersionString(str: String?) = values().find { it.versionString == str }

        @JvmStatic
        fun fromFullVersionString(str: String) =
            str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        // Version status
        //            1.0  1.1  1.2   1.3  1.4           1.5..1.7    1.8 1.9
        // Language:  UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL
        // API:       UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL

        @JvmField
        val FIRST_API_SUPPORTED = KOTLIN_1_3

        @JvmField
        val FIRST_SUPPORTED = KOTLIN_1_3

        @JvmField
        val FIRST_NON_DEPRECATED = KOTLIN_1_5

        @JvmField
        val LATEST_STABLE = KOTLIN_1_7
    }
}

interface LanguageOrApiVersion : DescriptionAware {
    val versionString: String

    val isStable: Boolean

    val isDeprecated: Boolean

    val isUnsupported: Boolean

    override val description: String
        get() = when {
            !isStable -> "$versionString (experimental)"
            isDeprecated -> "$versionString (deprecated)"
            isUnsupported -> "$versionString (unsupported)"
            else -> versionString
        }
}

fun LanguageVersion.isStableOrReadyForPreview(): Boolean =
    isStable

fun LanguageVersion.toKotlinVersion() = KotlinVersion(major, minor)

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature).let {
            it == LanguageFeature.State.ENABLED ||
                    it == LanguageFeature.State.ENABLED_WITH_WARNING
        }

    fun isPreRelease(): Boolean

    fun <T> getFlag(flag: AnalysisFlag<T>): T

    val apiVersion: ApiVersion

    // Please do not use this to enable/disable specific features/checks. Instead add a new LanguageFeature entry and call supportsFeature
    val languageVersion: LanguageVersion

    companion object {
        const val RESOURCE_NAME_TO_ALLOW_READING_FROM_ENVIRONMENT = "META-INF/allow-configuring-from-environment"
    }
}

class LanguageVersionSettingsImpl @JvmOverloads constructor(
    override val languageVersion: LanguageVersion,
    override val apiVersion: ApiVersion,
    analysisFlags: Map<AnalysisFlag<*>, Any?> = emptyMap(),
    specificFeatures: Map<LanguageFeature, LanguageFeature.State> = emptyMap()
) : LanguageVersionSettings {
    private val analysisFlags: Map<AnalysisFlag<*>, *> = Collections.unmodifiableMap(analysisFlags)
    private val specificFeatures: Map<LanguageFeature, LanguageFeature.State> = Collections.unmodifiableMap(specificFeatures)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getFlag(flag: AnalysisFlag<T>): T = analysisFlags[flag] as T? ?: flag.defaultValue

    override fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State {
        specificFeatures[feature]?.let { return it }

        val since = feature.sinceVersion
        if (since != null && languageVersion >= since && apiVersion >= feature.sinceApiVersion) {
            return feature.defaultState
        }

        return LanguageFeature.State.DISABLED
    }

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        specificFeatures.forEach { (feature, state) ->
            val char = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.ENABLED_WITH_WARNING -> '~'
                LanguageFeature.State.ENABLED_WITH_ERROR, LanguageFeature.State.DISABLED -> '-'
            }
            append(" $char$feature")
        }
        analysisFlags.forEach { (flag, value) ->
            append(" $flag:$value")
        }
    }

    override fun isPreRelease(): Boolean = !languageVersion.isStable ||
            specificFeatures.any { (feature, state) ->
                state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()
            }

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && kind.forcesPreReleaseBinaries
}
