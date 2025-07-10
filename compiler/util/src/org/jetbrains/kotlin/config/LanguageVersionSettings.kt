/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import org.jetbrains.kotlin.config.LanguageFeatureBehaviorAfterSinceVersion.CanStillBeDisabledForNow
import org.jetbrains.kotlin.config.LanguageFeatureBehaviorAfterSinceVersion.CannotBeDisabled
import org.jetbrains.kotlin.config.LanguageVersion.*
import org.jetbrains.kotlin.utils.DescriptionAware
import java.util.*

sealed class LanguageFeatureBehaviorAfterSinceVersion {
    data object CannotBeDisabled : LanguageFeatureBehaviorAfterSinceVersion()
    data class CanStillBeDisabledForNow(val relevantTicketId: String) : LanguageFeatureBehaviorAfterSinceVersion()
}

/**
 * @property sinceVersion determines in which Language Version the feature becomes enabled by default
 * @property sinceApiVersion determines minimal API Version required for using the feature
 * @property enabledInProgressiveMode
 * If 'true', then this feature will be automatically enabled under '-progressive' mode if `sinceKotlin` is set.
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
 * @property forcesPreReleaseBinaries
 * If 'true', then enabling this feature (e.g. by '-XXLanguage:', or dedicated '-X'-flag)
 * will force generation of pre-release binaries (given that [sinceVersion] > [LanguageVersion.LATEST_STABLE]).
 * Use it for features that involve generation of non-trivial low-level code with non-finalized design.
 *
 * @property testOnly
 * If 'true', then it's impossible to enable this feature using `-XXLanguage:+FeatureName` CLI flag.
 * Should be used for features which are already added to the compiler, but are not ready to be shown to users.
 *
 * @property [issue] YouTrack issue about the change related to specific feature
 * @property behaviorAfterSinceVersion set to [CanStillBeDisabledForNow] allows to disable specific feature with `-XXLanguage` flag
 *   even if the latest supported language version has this feature enabled by default.
 *   Should be used only in rare compatibility cases.
 */
enum class LanguageFeature(
    val sinceVersion: LanguageVersion?,
    val sinceApiVersion: ApiVersion = ApiVersion.KOTLIN_1_0,
    val issue: String,
    private val enabledInProgressiveMode: Boolean = false,
    val forcesPreReleaseBinaries: Boolean = false,
    val testOnly: Boolean = false,
    val hintUrl: String? = null,
    val behaviorAfterSinceVersion: LanguageFeatureBehaviorAfterSinceVersion = CannotBeDisabled,
) {
    // Note: names of these entries are also used in diagnostic tests and in user-visible messages (see presentableText below)

    // 1.1

    TypeAliases(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    BoundCallableReferences(KOTLIN_1_1, ApiVersion.KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    LocalDelegatedProperties(KOTLIN_1_1, ApiVersion.KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    TopLevelSealedInheritance(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    AdditionalBuiltInsMembers(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    DataClassInheritance(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    InlineProperties(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    DestructuringLambdaParameters(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    SingleUnderscoreForParameterName(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    DslMarkersSupport(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    UnderscoresInNumericLiterals(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    DivisionByZeroInConstantExpressions(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    InlineConstVals(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    OperatorProvideDelegate(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    ShortSyntaxForPropertyGetters(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    RefinedSamAdaptersPriority(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    SafeCallBoundSmartCasts(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    TypeInferenceOnGenericsForCallableReferences(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    NoDelegationToJavaDefaultInterfaceMembers(KOTLIN_1_1, NO_ISSUE_SPECIFIED),
    Coroutines(KOTLIN_1_1, ApiVersion.KOTLIN_1_1, NO_ISSUE_SPECIFIED),

    // 1.2

    InlineDefaultFunctionalParameters(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    SoundSmartCastsAfterTry(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    NullabilityAssertionOnExtensionReceiver(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    SafeCastCheckBoundSmartCasts(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    CapturedInClosureSmartCasts(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    LateinitTopLevelProperties(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    LateinitLocalVariables(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    InnerClassInEnumEntryClass(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    CallableReferencesToClassMembersWithEmptyLHS(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    JvmPackageName(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    AssigningArraysToVarargsInNamedFormInAnnotations(KOTLIN_1_2, NO_ISSUE_SPECIFIED),
    ExpectedTypeFromCast(KOTLIN_1_2, NO_ISSUE_SPECIFIED),

    // 1.3

    RestrictionOfValReassignmentViaBackingField(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    NestedClassesInEnumEntryShouldBeInner(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitDataClassesOverridingCopy(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    RestrictionOfWrongAnnotationsWithUseSiteTargetsOnTypes(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitInnerClassesOfGenericClassExtendingThrowable(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProperForInArrayLoopRangeVariableAssignmentSemantic(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    NestedClassesInAnnotations(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    JvmStaticInInterface(KOTLIN_1_3, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    JvmFieldInInterface(KOTLIN_1_3, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    ProhibitVisibilityOfNestedClassifiersFromSupertypesOfCompanion(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitNonConstValuesAsVarargsInAnnotations(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ReleaseCoroutines(KOTLIN_1_3, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    ReadDeserializedContracts(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    UseReturnsEffect(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    UseCallsInPlaceEffect(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    AllowContractsForCustomFunctions(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    VariableDeclarationInWhenSubject(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    ProhibitLocalAnnotations(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitSmartcastsOnLocalDelegatedProperty(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitAssigningSingleElementsToVarargsInNamedForm(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    FunctionTypesWithBigArity(KOTLIN_1_3, sinceApiVersion = ApiVersion.KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    RestrictRetentionForExpressionAnnotations(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    StrictJavaNullabilityAssertions(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    SoundSmartcastForEnumEntries(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitErroneousExpressionsInAnnotationsWithUseSiteTargets(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    NewCapturedReceiverFieldNamingConvention(KOTLIN_1_3, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ExtendedMainConvention(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    ExperimentalBuilderInference(KOTLIN_1_3, NO_ISSUE_SPECIFIED),
    InlineClasses(KOTLIN_1_3, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),

    // 1.4

    DslMarkerOnFunctionTypeReceiver(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    RestrictReturnStatementTarget(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    WarningOnMainUnusedParameter(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    PolymorphicSignature(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ProhibitConcurrentHashMapContains(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitTypeParametersForLocalVariables(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitJvmOverloadsOnConstructorsOfAnnotationClasses(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitTypeParametersInAnonymousObjects(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitRepeatedUseSiteTargetAnnotations(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitUseSiteTargetAnnotationsOnSuperTypes(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitTypeParametersInClassLiteralsInAnnotationArguments(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitComparisonOfIncompatibleEnums(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    BareArrayClassLiteral(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ProhibitGenericArrayClassLiteral(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    NonParenthesizedAnnotationsOnFunctionalTypes(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    UseGetterNameForPropertyAnnotationsMethodOnJvm(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    AllowBreakAndContinueInsideWhen(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    MixedNamedArgumentsInTheirOwnPosition(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ProhibitTailrecOnVirtualMember(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProperComputationOrderOfTailrecDefaultParameters(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    TrailingCommas(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ProhibitProtectedCallFromInline(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProperFinally(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    AllowAssigningArrayElementsToVarargsInNamedFormForFunctions(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    AllowNullOperatorsForResult(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    PreferJavaFieldOverload(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    AllowContractsForNonOverridableMembers(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    AllowReifiedGenericsInContracts(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ProperVisibilityForCompanionObjectInstanceField(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    DoNotGenerateThrowsForDelegatedKotlinMembers(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ProperIeee754Comparisons(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    FunctionalInterfaceConversion(KOTLIN_1_4, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    GenerateJvmOverloadsAsFinal(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    MangleClassMembersReturningInlineClasses(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ImproveReportingDiagnosticsOnProtectedMembersOfBaseClass(KOTLIN_1_4, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),

    NewInference(KOTLIN_1_4, NO_ISSUE_SPECIFIED),

    // In the next block, features can be enabled only along with new inference
    // v----------------------------------------------------------------------v
    SamConversionForKotlinFunctions(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    SamConversionPerArgument(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    FunctionReferenceWithDefaultValueAsOtherType(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    OverloadResolutionByLambdaReturnType(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    ContractsOnCallsWithImplicitReceiver(KOTLIN_1_4, NO_ISSUE_SPECIFIED),
    // ^----------------------------------------------------------------------^

    // 1.5

    ProhibitSpreadOnSignaturePolymorphicCall(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitInvisibleAbstractMethodsInSuperclasses(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitNonReifiedArraysAsReifiedTypeArguments(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitVarargAsArrayAfterSamArgument(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    CorrectSourceMappingSyntax(KOTLIN_1_5, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    RequiredPrimaryConstructorDelegationCallInEnums(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ApproximateAnonymousReturnTypesInPrivateInlineFunctions(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ForbidReferencingToUnderscoreNamedParameterOfCatchBlock(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    UseCorrectExecutionOrderForVarargArguments(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    JvmRecordSupport(KOTLIN_1_5, NO_ISSUE_SPECIFIED),
    AllowNullOperatorsForResultAndResultReturnTypeByDefault(KOTLIN_1_5, NO_ISSUE_SPECIFIED),
    AllowSealedInheritorsInDifferentFilesOfSamePackage(KOTLIN_1_5, NO_ISSUE_SPECIFIED),
    SealedInterfaces(KOTLIN_1_5, NO_ISSUE_SPECIFIED),
    JvmInlineValueClasses(KOTLIN_1_5, NO_ISSUE_SPECIFIED),
    SuspendFunctionsInFunInterfaces(KOTLIN_1_5, NO_ISSUE_SPECIFIED),
    SamWrapperClassesAreSynthetic(KOTLIN_1_5, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    StrictOnlyInputTypesChecks(KOTLIN_1_5, NO_ISSUE_SPECIFIED),

    // 1.6

    ProhibitJvmFieldOnOverrideFromInterfaceInPrimaryConstructor(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    PrivateInFileEffectiveVisibility(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitSelfCallsInNestedObjects(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProperCheckAnnotationsTargetInTypeUsePositions(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    SuspendFunctionAsSupertype(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    UnrestrictedBuilderInference(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    ClassTypeParameterAnnotations(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    WarnAboutNonExhaustiveWhenOnAlgebraicTypes(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    InstantiationOfAnnotationClasses(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    OptInContagiousSignatures(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    RepeatableAnnotations(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    RepeatableAnnotationContainerConstraints(KOTLIN_1_6, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    UseBuilderInferenceOnlyIfNeeded(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    SuspendConversion(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    ProhibitSuperCallsFromPublicInline(KOTLIN_1_6, NO_ISSUE_SPECIFIED),
    ProhibitProtectedConstructorCallFromPublicInline(KOTLIN_1_6, NO_ISSUE_SPECIFIED),

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
    TypeEnhancementImprovementsInStrictMode(KOTLIN_1_7, behaviorAfterSinceVersion = CanStillBeDisabledForNow("KT-76100"), issue = NO_ISSUE_SPECIFIED),
    OptInRelease(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    ProhibitNonExhaustiveWhenOnAlgebraicTypes(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    UseBuilderInferenceWithoutAnnotation(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    ProhibitSmartcastsOnPropertyFromAlienBaseClass(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitInvalidCharsInNativeIdentifiers(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    DefinitelyNonNullableTypes(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    ProhibitSimplificationOfNonTrivialConstBooleanExpressions(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    SafeCallsAreAlwaysNullable(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    JvmPermittedSubclassesAttributeForSealed(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    ProperTypeInferenceConstraintsProcessing(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ForbidExposingTypesInPrimaryConstructorProperties(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    PartiallySpecifiedTypeArguments(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    EliminateAmbiguitiesWithExternalTypeParameters(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    EliminateAmbiguitiesOnInheritedSamInterfaces(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    ProperInternalVisibilityCheckInImportingScope(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    InlineClassImplementationByDelegation(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    QualifiedSupertypeMayBeExtendedByOtherSupertype(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    YieldIsNoMoreReserved(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    NoDeprecationOnDeprecatedEnumEntries(KOTLIN_1_7, "KT-37975"),
    ProhibitQualifiedAccessToUninitializedEnumEntry(KOTLIN_1_7, enabledInProgressiveMode = true, "KT-41124"),
    ForbidRecursiveDelegateExpressions(KOTLIN_1_7, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    KotlinFunInterfaceConstructorReference(KOTLIN_1_7, NO_ISSUE_SPECIFIED),
    SuspendOnlySamConversions(KOTLIN_1_7, NO_ISSUE_SPECIFIED),

    // 1.8

    DontLoseDiagnosticsDuringOverloadResolutionByReturnType(KOTLIN_1_8, NO_ISSUE_SPECIFIED),
    ProhibitConfusingSyntaxInWhenBranches(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-48385"),
    UseConsistentRulesForPrivateConstructorsOfSealedClasses(sinceVersion = KOTLIN_1_8, enabledInProgressiveMode = true, "KT-44866"),
    ProgressionsChangingResolve(KOTLIN_1_8, "KT-49276"),
    AbstractClassMemberNotImplementedWithIntermediateAbstractClass(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-45508"),
    ForbidSuperDelegationToAbstractAnyMethod(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-38078"),
    ProperEqualityChecksInBuilderInferenceCalls(KOTLIN_1_8, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitNonExhaustiveIfInRhsOfElvis(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-44705"),
    ReportMissingUpperBoundsViolatedErrorOnAbbreviationAtSupertypes(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-29168"),
    ForbidUsingExtensionPropertyTypeParameterInDelegate(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-24643"),
    SynchronizedSuspendError(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-48516"),
    ReportNonVarargSpreadOnGenericCalls(KOTLIN_1_8, enabledInProgressiveMode = true, "KT-48162"),
    RangeUntilOperator(KOTLIN_1_8, "KT-15613"),
    GenericInlineClassParameter(sinceVersion = KOTLIN_1_8, forcesPreReleaseBinaries = true, issue = "KT-32162"),

    // 1.9

    ProhibitIllegalValueParameterUsageInDefaultArguments(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-25694"),
    ProhibitConstructorCallOnFunctionalSupertype(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-46344"),
    ProhibitArrayLiteralsInCompanionOfAnnotation(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-39041"),
    ProhibitCyclesInAnnotations(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-47932"),
    ForbidExtensionFunctionTypeOnNonFunctionTypes(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-43527"),
    ProhibitEnumDeclaringClass(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-49653"),
    StopPropagatingDeprecationThroughOverrides(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-47902"),
    ReportTypeVarianceConflictOnQualifierArguments(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-50947"),
    ReportErrorsOnRecursiveTypeInsidePlusAssignment(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-48546"),
    ForbidExtensionCallsOnInlineFunctionalParameters(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-52502"),
    SkipStandaloneScriptsInSourceRoots(KOTLIN_1_9, "KT-52525"),
    ModifierNonBuiltinSuspendFunError(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-49264"),
    EnumEntries(KOTLIN_1_9, sinceApiVersion = ApiVersion.KOTLIN_1_8, forcesPreReleaseBinaries = true, issue = "KT-48872"),
    ForbidSuperDelegationToAbstractFakeOverride(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-49017"),
    DataObjects(KOTLIN_1_9, "KT-4107"),
    ProhibitAccessToEnumCompanionMembersInEnumConstructorCall(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-49110"),
    RefineTypeCheckingOnAssignmentsToJavaFields(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-46727"),
    ValueClassesSecondaryConstructorWithBody(sinceVersion = KOTLIN_1_9, forcesPreReleaseBinaries = true, issue = "KT-55333"),
    NativeJsProhibitLateinitIsInitializedIntrinsicWithoutPrivateAccess(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-27002"),
    TakeIntoAccountEffectivelyFinalInMustBeInitializedCheck(KOTLIN_1_9, "KT-58587"),
    ProhibitUsingNullableTypeParameterAgainstNotNullAnnotated(sinceVersion = KOTLIN_1_9, "KT-36770"),
    NoSourceCodeInNotNullAssertionExceptions(KOTLIN_1_9, sinceApiVersion = ApiVersion.KOTLIN_1_4, "KT-57570"),

    // 1.9.20 KMP stabilization. Unfortunately, we don't have 1.9.20 LV. So LV=1.9 is the best we can do.
    // At least there won't be false positives for 1.8 users
    MultiplatformRestrictions(KOTLIN_1_9, enabledInProgressiveMode = true, "KT-61668"),

    // End of 1.* language features --------------------------------------------------

    // 2.0

    EnhanceNullabilityOfPrimitiveArrays(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-54521"),

    /**
     * This feature is highly related to ForbidInferringTypeVariablesIntoEmptyIntersection and while they belong to the same LV,
     * they might be used interchangeably.
     *
     * But there might be the case that we may postpone ForbidInferringTypeVariablesIntoEmptyIntersection but leave AllowEmptyIntersectionsInResultTypeResolver in 2.0.
     * In that case, we would stick to the simple behavior of just inferring empty intersection (without complicated logic of filtering out expected constraints),
     * but we would report a warning instead of an error (until ForbidInferringTypeVariablesIntoEmptyIntersection is enabled).
     */
    AllowEmptyIntersectionsInResultTypeResolver(KOTLIN_2_0, "KT-51221"),
    ProhibitSmartcastsOnPropertyFromAlienBaseClassInheritedInInvisibleClass(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-57290"),
    ForbidInferringPostponedTypeVariableIntoDeclaredUpperBound(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-47986"),
    ProhibitUseSiteGetTargetAnnotations(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-15470"),
    KeepNullabilityWhenApproximatingLocalType(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-53982"),
    ProhibitAccessToInvisibleSetterFromDerivedClass(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-56662"),
    ProhibitOpenValDeferredInitialization(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-57553"),
    SupportEffectivelyFinalInExpectActualVisibilityCheck(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-61955"),
    ProhibitMissedMustBeInitializedWhenThereIsNoPrimaryConstructor(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-58472"),
    MangleCallsToJavaMethodsWithValueClasses(KOTLIN_2_0, "KT-55945"),
    ProhibitDefaultArgumentsInExpectActualizedByFakeOverride(KOTLIN_2_0, enabledInProgressiveMode = true, "KT-62036"),
    DisableCompatibilityModeForNewInference(KOTLIN_2_0, "KT-63558"), // KT-63558 (umbrella), KT-64306, KT-64307, KT-64308
    DfaBooleanVariables(KOTLIN_2_0, "KT-25747"),
    LightweightLambdas(KOTLIN_2_0, "KT-45375"),
    ObjCSignatureOverrideAnnotation(KOTLIN_2_0, sinceApiVersion = ApiVersion.KOTLIN_2_0, "KT-61323"),

    // 2.1

    ProhibitImplementingVarByInheritedVal(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-56779"),
    PrioritizedEnumEntries(KOTLIN_2_1, forcesPreReleaseBinaries = true, issue = "KT-58920"),
    ProhibitInlineModifierOnPrimaryConstructorParameters(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-59664"),
    ProhibitSingleNamedFunctionAsExpression(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-62573"),
    ForbidLambdaParameterWithMissingDependencyType(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-64474"),
    JsAllowInvalidCharsIdentifiersEscaping(KOTLIN_2_1, "KT-31799"),
    SupportJavaErrorEnhancementOfArgumentsOfWarningLevelEnhanced(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-63209"),
    ProhibitPrivateOperatorCallInInline(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-65494"),
    ProhibitTypealiasAsCallableQualifierInImport(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-64350"),
    JsExternalPropertyParameters(KOTLIN_2_1, "KT-65965"),
    CorrectSpecificityCheckForSignedAndUnsigned(KOTLIN_2_1, "KT-35305"),
    AllowAccessToProtectedFieldFromSuperCompanion(KOTLIN_2_1, "KT-39868"),
    CheckLambdaAgainstTypeVariableContradictionInResolution(KOTLIN_2_1, "KT-58310"),
    ProperUninitializedEnumEntryAccessAnalysis(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-68451"),
    ImprovedCapturedTypeApproximationInInference(KOTLIN_2_1, "KT-64515"),
    ImprovedVarianceInCst(KOTLIN_2_1, "KT-68970"),
    InferMoreImplicationsFromBooleanExpressions(KOTLIN_2_1, "KT-64193"),
    ImprovedExhaustivenessChecksIn21(KOTLIN_2_1, "KT-21908"),
    ProhibitSynchronizationByValueClassesAndPrimitives(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-67791"),
    AllowSuperCallToJavaInterface(KOTLIN_2_1, "KT-69729"),
    ProhibitJavaClassInheritingPrivateKotlinClass(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-66328"),
    ProhibitReturningIncorrectNullabilityValuesFromSamConstructorLambdaOfJdkInterfaces(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-57014"),
    ProhibitNothingAsCatchParameter(KOTLIN_2_1, enabledInProgressiveMode = true, "KT-8322"),
    NullableNothingInReifiedPosition(KOTLIN_2_1, forcesPreReleaseBinaries = true, issue = "KT-54227"), // KT-54227, KT-67675
    ElvisInferenceImprovementsIn21(KOTLIN_2_1, "KT-71751"),
    // TODO: Remove org.jetbrains.kotlin.fir.resolve.calls.stages.ConstraintSystemForks together with this LF (KT-72961)
    ConsiderForkPointsWhenCheckingContradictions(KOTLIN_2_1, "KT-68768"),

    // It's not a fully blown LF, but mostly a way to manage potential unexpected semantic changes
    // See the single usage at org.jetbrains.kotlin.fir.types.ConeTypeApproximator.fastPathSkipApproximation
    AvoidApproximationOfRecursiveCapturedTypesWithNoReason(KOTLIN_2_1, "KT-69995"),
    PCLAEnhancementsIn21(KOTLIN_2_1, "KT-69170"),

    // Common feature for all non-PCLA inference enhancements in 2.1
    InferenceEnhancementsIn21(KOTLIN_2_1, "KT-61227"),

    // It's not a fully blown LF, but mostly a way to manage potential unexpected semantic changes
    // See the single usage at org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintIncorporator.computeNewDerivedFrom
    // We enable it for already released 2.1 because it's a bug fix
    StricterConstraintIncorporationRecursionDetector(KOTLIN_2_1, "KT-73434"),

    // It's not a fully blown LF, but mostly a way to manage potential unexpected semantic changes
    // See the single usage at org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintInjector.TypeCheckerStateForConstraintInjector.runForkingPoint
    // We enable it for already released 2.1 because it's a bug fix
    ForkIsNotSuccessfulWhenNoBranchIsSuccessful(KOTLIN_2_1, "KT-75444"),

    // 2.2

    BreakContinueInInlineLambdas(KOTLIN_2_2, "KT-1436"),
    ForbidUsingExpressionTypesWithInaccessibleContent(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-66691"),
    ReportExposedTypeForMoreCasesOfTypeParameterBounds(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-69653"),
    ForbidReifiedTypeParametersOnTypeAliases(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-70163"),
    ForbidProjectionsInAnnotationProperties(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-70002"),
    ForbidJvmAnnotationsOnAnnotationParameters(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-25861"),
    ForbidFieldAnnotationsOnAnnotationParameters(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-70233"),
    ProhibitConstructorAndSupertypeOnTypealiasWithTypeProjection(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-60305"),
    CallableReferenceOverloadResolutionInLambda(KOTLIN_2_2, "KT-73011"),
    ProhibitGenericQualifiersOnConstructorCalls(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-73527"),
    AvoidWrongOptimizationOfTypeOperatorsOnValueClasses(KOTLIN_2_2, "KT-67517"), // KT-67517, KT-67518, KT-67520
    ForbidSyntheticPropertiesWithoutBaseJavaGetter(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-72305"), // KT-72305, KT-64358
    AnnotationDefaultTargetMigrationWarning(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-73255"), // KT-73255, KT-73494
    AllowDnnTypeOverridingFlexibleType(KOTLIN_2_2, "KT-74049"),
    ForbidEnumEntryNamedEntries(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-72829"), // KT-72829, KT-58920
    WhenGuards(KOTLIN_2_2, "KT-13626"),
    MultiDollarInterpolation(KOTLIN_2_2, "KT-2425"),
    JvmDefaultEnableByDefault(KOTLIN_2_2, "KT-71768"),
    ForbidExposureOfPrivateTypesInNonPrivateInlineFunctionsInKlibs(sinceVersion = KOTLIN_2_2, enabledInProgressiveMode = true, "KT-70916"),
    FixationEnhancementsIn22(KOTLIN_2_2, "KT-76345"), // KT-76345, KT-71854
    ForbidCrossFileIrFieldAccessInKlibs(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-71138"),
    AllowExpectDeclarationsInJsExport(KOTLIN_2_2, "KT-64951"),
    DoNotRunSuspendConversionForLambdaReturnStatements(KOTLIN_2_2, enabledInProgressiveMode = true, "KT-74932"),

    // 2.3

    ForbidCompanionInLocalInnerClass(KOTLIN_2_3, enabledInProgressiveMode = true, "KT-47289"),
    ForbidImplementationByDelegationWithDifferentGenericSignature(KOTLIN_2_3, enabledInProgressiveMode = true, "KT-72140"),
    ForbidJvmSerializableLambdaOnInlinedFunctionLiterals(KOTLIN_2_3, enabledInProgressiveMode = true, "KT-71906"),
    ReportExposedTypeForInternalTypeParameterBounds(KOTLIN_2_3, enabledInProgressiveMode = true, "KTLC-275"),
    EnableDfaWarningsInK2(KOTLIN_2_3, "KT-50965"),
    ForbidParenthesizedLhsInAssignments(KOTLIN_2_3, enabledInProgressiveMode = true, "KT-70507"),
    DontMakeExplicitJavaTypeArgumentsFlexible(KOTLIN_2_3, "KTLC-284"),
    PreciseSimplificationToFlexibleLowerConstraint(KOTLIN_2_3, "KT-78621"),
    DontIgnoreUpperBoundViolatedOnImplicitArguments(KOTLIN_2_3, "KT-67146"),
    ResolveTopLevelLambdasAsSyntheticCallArgument(KOTLIN_2_3, "KT-67869"),
    DataFlowBasedExhaustiveness(sinceVersion = KOTLIN_2_3, issue = "KT-76635"),
    UnstableSmartcastOnDelegatedProperties(KOTLIN_2_3, enabledInProgressiveMode = true, "KTLC-273"),
    ForbidAnnotationsWithUseSiteTargetOnExpressions(KOTLIN_2_3, enabledInProgressiveMode = true, "KT-75242"),
    ProhibitNullableTypeThroughTypealias(KOTLIN_2_3, enabledInProgressiveMode = true, "KTLC-279"),
    ForbidObjectDelegationToItself(KOTLIN_2_3, enabledInProgressiveMode = true, "KT-17417"),
    JvmIndyAllowLambdasWithAnnotations(KOTLIN_2_3, "KT-76606"),

    AllowCheckForErasedTypesInContracts(KOTLIN_2_3, "KT-45683"),
    AllowContractsOnSomeOperators(KOTLIN_2_3, "KT-32313"),
    AllowContractsOnPropertyAccessors(KOTLIN_2_3, "KT-27090"),
    ConditionImpliesReturnsContracts(KOTLIN_2_3, "KT-8889"),
    HoldsInContracts(KOTLIN_2_3, "KT-32993"),

    InferenceEnhancementsIn23(KOTLIN_2_3, "KT-76826"),
    AllowReturnInExpressionBodyWithExplicitType(KOTLIN_2_3, "KT-76926"),
    ParseLambdaWithSuspendModifier(KOTLIN_2_3, "KT-22765"),
    DiscriminateSuspendInOverloadResolution(KOTLIN_2_3, "KT-23610"),

    // 2.4

    ForbidExposingLessVisibleTypesInInline(KOTLIN_2_4, enabledInProgressiveMode = true, "KTLC-283"),
    ForbidCaptureInlinableLambdasInJsCode(KOTLIN_2_4, enabledInProgressiveMode = true, "KT-69297"),
    ForbidInitializationBeforeDeclarationInAnonymous(KOTLIN_2_4, enabledInProgressiveMode = true, "KT-77156"),
    AllowReifiedTypeInCatchClause(KOTLIN_2_4, issue = "KT-54363"),
    ForbidGetSetValueWithTooManyParameters(KOTLIN_2_4, issue = "KT-77131"),
    ForbidReturnInExpressionBodyWithoutExplicitTypeEdgeCases(KOTLIN_2_4, enabledInProgressiveMode = true, "KTLC-288"),
    CheckOptInOnPureEnumEntries(KOTLIN_2_4, enabledInProgressiveMode = true, "KTLC-359"),
    ForbidExposingPackagePrivateInInternal(KOTLIN_2_4, enabledInProgressiveMode = true, "KTLC-271"),

    // 2.5

    ErrorAboutDataClassCopyVisibilityChange(KOTLIN_2_5, enabledInProgressiveMode = true, "KT-11914"), // KT-11914. Deprecation phase 2

    // End of 2.* language features --------------------------------------------------

    ExpectActualClasses(sinceVersion = null, "KT-62885"),

    DataClassCopyRespectsConstructorVisibility(sinceVersion = null, "KT-11914"), // KT-11914 Deprecation phase 3

    DirectJavaActualization(sinceVersion = null, "KT-67202"),

    // Disabled for indefinite time. See KT-53751
    IgnoreNullabilityForErasedValueParameters(sinceVersion = null, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),

    // Disabled for indefinite time. Disables restrictions of builder inference without annotation
    // Note: In 1.7.0, builder inference without annotation was introduced.
    // However, later we encountered various situations when it works incorrectly, and decided to forbid them.
    // When this feature is disabled, various errors are reported which are related to these incorrect situations.
    // When this feature is enabled, no such errors are reported.
    NoBuilderInferenceWithoutAnnotationRestriction(sinceVersion = null, NO_ISSUE_SPECIFIED),

    // Disabled for indefinite time. Forces K2 report errors (instead of warnings) for incompatible
    // equality & identity operators in cases where K1 would report warnings or would not report anything.
    ReportErrorsForComparisonOperators(sinceVersion = null, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),

    // Disabled for indefinite time.
    // Disables reporting of new errors (see KT-55055, KT-55056, KT-55079) in DiagnosticReporterByTrackingStrategy.
    // All these errors are "lost" errors which existed always, but wasn't reported before 1.9.0.
    // When this feature is disabled, all these "lost" errors are reported properly.
    // When this feature is enabled, no such errors are reported.
    NoAdditionalErrorsInK1DiagnosticReporter(sinceVersion = null, NO_ISSUE_SPECIFIED),

    // top-level script inner classes never made any sense, but used for some time to overcome the capturing logic limitations
    // Now capturing logic works properly, therefore the warning is reported in K2
    // this feature will eventually switch this warning to an error
    ProhibitScriptTopLevelInnerClasses(sinceVersion = null, NO_ISSUE_SPECIFIED),

    // Experimental features

    ExpectRefinement(sinceVersion = null, "KT-73557"),
    JsEnableExtensionFunctionInExternals(sinceVersion = null, NO_ISSUE_SPECIFIED),
    PackagePrivateFileClassesWithAllPrivateMembers(sinceVersion = null, NO_ISSUE_SPECIFIED), // Disabled until the breaking change is approved by the committee, see KT-10884.
    MultiPlatformProjects(sinceVersion = null, NO_ISSUE_SPECIFIED),
    ProhibitComparisonOfIncompatibleClasses(sinceVersion = null, enabledInProgressiveMode = true, NO_ISSUE_SPECIFIED),
    ProhibitAllMultipleDefaultsInheritedFromSupertypes(sinceVersion = null, enabledInProgressiveMode = false, NO_ISSUE_SPECIFIED),
    ProhibitIntersectionReifiedTypeParameter(sinceVersion = null, enabledInProgressiveMode = true, "KT-71420"),
    ExplicitBackingFields(sinceVersion = null, forcesPreReleaseBinaries = true, issue = "KT-14663"),
    FunctionalTypeWithExtensionAsSupertype(sinceVersion = null, NO_ISSUE_SPECIFIED),
    JsAllowValueClassesInExternals(sinceVersion = null, NO_ISSUE_SPECIFIED),
    ContextReceivers(sinceVersion = null, NO_ISSUE_SPECIFIED),
    ContextParameters(sinceVersion = null, "KT-72222"),
    ValueClasses(sinceVersion = null, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    JavaSamConversionEqualsHashCode(sinceVersion = null, forcesPreReleaseBinaries = true, issue = NO_ISSUE_SPECIFIED),
    PropertyParamAnnotationDefaultTargetMode(sinceVersion = null, "KT-73255"),
    AnnotationAllUseSiteTarget(sinceVersion = null, "KT-73256"),
    ImplicitJvmExposeBoxed(sinceVersion = null, forcesPreReleaseBinaries = true, issue = "KT-73466"),

    // K1 support only. We keep it, as we may want to support it also in K2
    UnitConversionsOnArbitraryExpressions(sinceVersion = null, NO_ISSUE_SPECIFIED),

    JsAllowImplementingFunctionInterface(sinceVersion = null, NO_ISSUE_SPECIFIED),
    CustomEqualsInValueClasses(sinceVersion = null, "KT-24874"),
    ContractSyntaxV2(sinceVersion = null, forcesPreReleaseBinaries = true, issue = "KT-56127"),
    ReferencesToSyntheticJavaProperties(sinceVersion = null, testOnly = true, issue = "KT-8575"),
    ImplicitSignedToUnsignedIntegerConversion(sinceVersion = null, testOnly = true, issue = "KT-56583"),
    ForbidInferringTypeVariablesIntoEmptyIntersection(sinceVersion = null, enabledInProgressiveMode = true, "KT-51221"),
    IntrinsicConstEvaluation(sinceVersion = null, testOnly = true, issue = "KT-49303"),

    // K1 support only. We keep it, as it's currently unclear what to do with this feature in K2
    DisableCheckingChangedProgressionsResolve(sinceVersion = null, "KT-49276"),

    DontCreateSyntheticPropertiesWithoutBaseJavaGetter(sinceVersion = null, "KT-64358"),
    JavaTypeParameterDefaultRepresentationWithDNN(sinceVersion = null, testOnly = true, issue = "KT-59138"),
    ProperFieldAccessGenerationForFieldAccessShadowedByKotlinProperty(sinceVersion = null, "KT-56386"),
    IrInlinerBeforeKlibSerialization(sinceVersion = null, forcesPreReleaseBinaries = true, issue = "KT-69765"),
    NestedTypeAliases(sinceVersion = null, forcesPreReleaseBinaries = true, issue = "KT-45285"),
    ForbidUsingSupertypesWithInaccessibleContentInTypeArguments(sinceVersion = null, enabledInProgressiveMode = true, "KT-66691"), // KT-66691, KT-66742
    AllowEagerSupertypeAccessibilityChecks(sinceVersion = null, enabledInProgressiveMode = true, "KT-73611"),
    UnnamedLocalVariables(sinceVersion = null, forcesPreReleaseBinaries = false, issue = "KT-74809"),
    ContextSensitiveResolutionUsingExpectedType(sinceVersion = null, "KT-16768"),
    AnnotationsInMetadata(sinceVersion = null, "KT-57919"),
    DisableWarningsForValueBasedJavaClasses(sinceVersion = null, "KT-70722"),
    DisableWarningsForIdentitySensitiveOperationsOnValueClassesAndPrimitives(sinceVersion = null, "KT-70722"),
    IrRichCallableReferencesInKlibs(sinceVersion = null, "KT-72734"), // KT-72734, KT-74384, KT-74392
    ExportKlibToOlderAbiVersion(sinceVersion = null, forcesPreReleaseBinaries = true, issue = "KT-76131"),
    ForbidInferOfInvisibleTypeAsReifiedVarargOrReturnType(sinceVersion = null, enabledInProgressiveMode = true, issue = "KTLC-14"),
    ;

    constructor(
        sinceVersion: LanguageVersion?,
        issue: String
    ) : this(sinceVersion, sinceApiVersion = ApiVersion.KOTLIN_1_0, issue)

    constructor(
        sinceVersion: LanguageVersion?,
        enabledInProgressiveMode: Boolean,
        issue: String
    ) : this(sinceVersion, sinceApiVersion = ApiVersion.KOTLIN_1_0, issue, enabledInProgressiveMode = enabledInProgressiveMode)

    init {
        if (testOnly && sinceVersion != null) {
            error("$this: should be enabled by default since version $sinceVersion but is test only")
        }
    }

    val presentableName: String
        // E.g. "DestructuringLambdaParameters" -> ["Destructuring", "Lambda", "Parameters"] -> "destructuring lambda parameters"
        get() = name.split("(?<!^)(?=[A-Z])".toRegex()).joinToString(separator = " ", transform = String::lowercase)

    val presentableText get() = if (hintUrl == null) presentableName else "$presentableName (See: $hintUrl)"

    enum class State(override val description: String) : DescriptionAware {
        ENABLED("Enabled"),
        DISABLED("Disabled");
    }

    /**
     * If 'true', then this feature will be automatically enabled under '-progressive' mode.
     *
     * Please, see [enabledInProgressiveMode] in [LanguageFeature] for more details.
     */
    val actuallyEnabledInProgressiveMode: Boolean get() = enabledInProgressiveMode && sinceVersion != null

    companion object {
        @JvmStatic
        fun fromString(str: String) = entries.find { it.name == str }
    }
}

/**
 * Placeholder for old language features for which the ticket was not specified.
 * Please never use it for new features.
 */
const val NO_ISSUE_SPECIFIED = "No YT issue"

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

    KOTLIN_2_0(2, 0),
    KOTLIN_2_1(2, 1),
    KOTLIN_2_2(2, 2),
    KOTLIN_2_3(2, 3),
    KOTLIN_2_4(2, 4),
    KOTLIN_2_5(2, 5),
    ;

    override val isStable: Boolean
        get() = this <= LATEST_STABLE

    val usesK2: Boolean
        get() = this >= KOTLIN_2_0

    override val isDeprecated: Boolean
        get() = FIRST_SUPPORTED <= this && this < FIRST_NON_DEPRECATED

    override val isUnsupported: Boolean
        get() = this < FIRST_SUPPORTED

    override val versionString: String = "$major.$minor"

    override fun toString() = versionString

    companion object {
        @JvmStatic
        fun fromVersionString(str: String?) = entries.find { it.versionString == str }

        @JvmStatic
        fun fromFullVersionString(str: String) =
            str.split(".", "-").let { if (it.size >= 2) fromVersionString("${it[0]}.${it[1]}") else null }

        // Version status
        //            1.0..1.7        1.8..1.9           2.0..2.2    2.3..2.5
        // Language:  UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL
        // API:       UNSUPPORTED --> DEPRECATED ------> STABLE ---> EXPERIMENTAL

        @JvmField
        val FIRST_API_SUPPORTED = KOTLIN_1_8

        @JvmField
        val FIRST_SUPPORTED = KOTLIN_1_8

        @JvmField
        val FIRST_NON_DEPRECATED = KOTLIN_2_0

        @JvmField
        val LATEST_STABLE = KOTLIN_2_2
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

// This is a public API used in IDEA kotlin plugin code, in particular in
// community/plugins/kotlin/base/compiler-configuration-ui/src/org/jetbrains/kotlin/idea/base/compilerPreferences/configuration/KotlinCompilerConfigurableTab.java
@Suppress("unused")
@Deprecated(
    message = "This function is no more actual after 2.0 release, consider replacing with isStable",
    replaceWith = ReplaceWith("isStable")
)
fun LanguageVersion.isStableOrReadyForPreview(): Boolean =
    isStable || this == KOTLIN_1_9 || this == KOTLIN_2_0

fun LanguageVersion.toKotlinVersion() = KotlinVersion(major, minor)
fun LanguageVersionSettings.toKotlinVersion() = languageVersion.toKotlinVersion()

interface LanguageVersionSettings {
    fun getFeatureSupport(feature: LanguageFeature): LanguageFeature.State

    fun supportsFeature(feature: LanguageFeature): Boolean =
        getFeatureSupport(feature) == LanguageFeature.State.ENABLED

    fun getManuallyEnabledLanguageFeatures(): List<LanguageFeature>

    fun getManuallyDisabledLanguageFeatures(): List<LanguageFeature>

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

        return if (isEnabledByDefault(feature)) {
            LanguageFeature.State.ENABLED
        } else {
            LanguageFeature.State.DISABLED
        }
    }

    override fun getManuallyEnabledLanguageFeatures(): List<LanguageFeature> =
        specificFeatures.filter { isEnabledOnlyByFlag(it.key, it.value) }.keys.toList()

    override fun getManuallyDisabledLanguageFeatures(): List<LanguageFeature> =
        specificFeatures.filter { isDisabledOnlyByFlag(it.key, it.value) }.keys.toList()

    private fun isEnabledOnlyByFlag(feature: LanguageFeature, state: LanguageFeature.State): Boolean =
        !isEnabledByDefault(feature) && (state == LanguageFeature.State.ENABLED)

    private fun isDisabledOnlyByFlag(feature: LanguageFeature, state: LanguageFeature.State): Boolean =
        isEnabledByDefault(feature) && state == LanguageFeature.State.DISABLED

    private fun isEnabledByDefault(feature: LanguageFeature): Boolean =
        feature.sinceVersion != null && languageVersion >= feature.sinceVersion && apiVersion >= feature.sinceApiVersion

    override fun toString() = buildString {
        append("Language = $languageVersion, API = $apiVersion")
        specificFeatures.entries.sortedBy { (feature, _) -> feature.ordinal }.forEach { (feature, state) ->
            val char = when (state) {
                LanguageFeature.State.ENABLED -> '+'
                LanguageFeature.State.DISABLED -> '-'
            }
            append(" $char$feature")
        }
        analysisFlags.entries.sortedBy { (flag, _) -> flag.toString() }.forEach { (flag, value) ->
            append(" $flag:$value")
        }
    }

    override fun isPreRelease(): Boolean = languageVersion.isPreRelease() ||
            specificFeatures.any { (feature, state) ->
                state == LanguageFeature.State.ENABLED && feature.forcesPreReleaseBinariesIfEnabled()
            }

    companion object {
        @JvmField
        val DEFAULT = LanguageVersionSettingsImpl(LanguageVersion.LATEST_STABLE, ApiVersion.LATEST_STABLE)
    }
}

fun LanguageVersion.isPreRelease(): Boolean {
    if (!isStable) return true

    return KotlinCompilerVersion.isPreRelease() && this == LanguageVersion.LATEST_STABLE
}

fun LanguageFeature.forcesPreReleaseBinariesIfEnabled(): Boolean {
    val isFeatureNotReleasedYet = sinceVersion?.isStable != true
    return isFeatureNotReleasedYet && forcesPreReleaseBinaries
}
