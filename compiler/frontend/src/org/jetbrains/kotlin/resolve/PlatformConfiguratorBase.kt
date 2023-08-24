/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMapper
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.lazy.AbsentDescriptorHandler
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.types.DynamicTypesSettings

private val DEFAULT_DECLARATION_CHECKERS = listOf(
    ExpectActualInTheSameModuleChecker,
    ActualClassifierMustHasTheSameMembersAsNonFinalExpectClassifierChecker,
    DataClassDeclarationChecker(),
    ConstModifierChecker,
    UnderscoreChecker,
    InlineParameterChecker,
    InfixModifierChecker(),
    SinceKotlinAnnotationValueChecker,
    RequireKotlinAnnotationValueChecker,
    ReifiedTypeParameterAnnotationChecker(),
    DynamicReceiverChecker,
    DelegationChecker(),
    KClassWithIncorrectTypeArgumentChecker,
    SuspendLimitationsChecker,
    ValueClassDeclarationChecker,
    MultiFieldValueClassAnnotationsChecker,
    PropertiesWithBackingFieldsInsideValueClass(),
    InnerClassInsideValueClass(),
    AnnotationClassTargetAndRetentionChecker(),
    ReservedMembersAndConstructsForValueClass(),
    ResultClassInReturnTypeChecker(),
    LocalVariableTypeParametersChecker(),
    ExplicitApiDeclarationChecker(),
    TailrecFunctionChecker,
    TrailingCommaDeclarationChecker,
    MissingDependencySupertypeChecker.ForDeclarations,
    FunInterfaceDeclarationChecker(),
    DeprecationInheritanceChecker,
    DeprecatedSinceKotlinAnnotationChecker,
    ContractDescriptionBlockChecker,
    PrivateInlineFunctionsReturningAnonymousObjectsChecker,
    SealedInheritorInSamePackageChecker,
    SealedInheritorInSameModuleChecker,
    SealedInterfaceAllowedChecker,
    SuspendFunctionAsSupertypeChecker,
    EnumCompanionInEnumConstructorCallChecker,
    ContextualDeclarationChecker,
    SubtypingBetweenContextReceiversChecker,
    ValueParameterUsageInDefaultArgumentChecker,
    CyclicAnnotationsChecker,
    UnsupportedUntilRangeDeclarationChecker,
    DataObjectContentChecker,
    EnumEntriesRedeclarationChecker,
    VolatileAnnotationChecker,
    ActualTypealiasToSpecialAnnotationChecker,
)

private val DEFAULT_CALL_CHECKERS = listOf(
    CapturingInClosureChecker(), InlineCheckerWrapper(), SynchronizedByValueChecker(), SafeCallChecker(), TrailingCommaCallChecker,
    DeprecatedCallChecker, CallReturnsArrayOfNothingChecker(), InfixCallChecker(), OperatorCallChecker(),
    ConstructorHeaderCallChecker, ProtectedConstructorCallChecker, ApiVersionCallChecker,
    CoroutineSuspendCallChecker, BuilderFunctionsCallChecker, DslScopeViolationCallChecker, MissingDependencyClassChecker,
    CallableReferenceCompatibilityChecker(),
    UnderscoreUsageChecker, AssigningNamedArgumentToVarargChecker(), ImplicitNothingAsTypeParameterCallChecker,
    PrimitiveNumericComparisonCallChecker, LambdaWithSuspendModifierCallChecker,
    UselessElvisCallChecker(), ResultTypeWithNullableOperatorsChecker(), NullableVarargArgumentCallChecker,
    NamedFunAsExpressionChecker, ContractNotAllowedCallChecker, ReifiedTypeParameterSubstitutionChecker(),
    MissingDependencySupertypeChecker.ForCalls, AbstractClassInstantiationChecker, SuspendConversionCallChecker,
    UnitConversionCallChecker, FunInterfaceConstructorReferenceChecker, NullableExtensionOperatorWithSafeCallChecker,
    ReferencingToUnderscoreNamedParameterOfCatchBlockChecker, VarargWrongExecutionOrderChecker, SelfCallInNestedObjectConstructorChecker,
    NewSchemeOfIntegerOperatorResolutionChecker, EnumEntryVsCompanionPriorityCallChecker, CompanionInParenthesesLHSCallChecker,
    ResolutionToPrivateConstructorOfSealedClassChecker, EqualityCallChecker, UnsupportedUntilOperatorChecker,
    BuilderInferenceAssignmentChecker, IncorrectCapturedApproximationCallChecker, CompanionIncorrectlyUnboundedWhenUsedAsLHSCallChecker,
    CustomEnumEntriesMigrationCallChecker, EnumEntriesUnsupportedChecker
)
private val DEFAULT_TYPE_CHECKERS = emptyList<AdditionalTypeChecker>()
private val DEFAULT_CLASSIFIER_USAGE_CHECKERS = listOf(
    DeprecatedClassifierUsageChecker(), ApiVersionClassifierUsageChecker, MissingDependencyClassChecker.ClassifierUsage,
    OptionalExpectationUsageChecker()
)
private val DEFAULT_ANNOTATION_CHECKERS = listOf<AdditionalAnnotationChecker>()

private val DEFAULT_CLASH_RESOLVERS = listOf<PlatformExtensionsClashResolver<*>>(
    IdentifierCheckerClashesResolver(),

    /**
     * We should use NONE for clash resolution, because:
     * - JvmTypeSpecificityComparator covers cases with flexible types and primitive types loaded from Java, and all this is irrelevant for
     *   non-JVM modules
     * - JsTypeSpecificityComparator covers case with dynamics, which are not allowed in non-JS modules either
     */
    PlatformExtensionsClashResolver.FallbackToDefault(TypeSpecificityComparator.NONE, TypeSpecificityComparator::class.java),

    PlatformExtensionsClashResolver.FallbackToDefault(DynamicTypesSettings(), DynamicTypesSettings::class.java),

    PlatformExtensionsClashResolver.FirstWins(AbsentDescriptorHandler::class.java),

    PlatformDiagnosticSuppressorClashesResolver()
)

fun StorageComponentContainer.configureDefaultCheckers() {
    DEFAULT_DECLARATION_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CALL_CHECKERS.forEach { useInstance(it) }
    DEFAULT_TYPE_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CLASSIFIER_USAGE_CHECKERS.forEach { useInstance(it) }
    DEFAULT_ANNOTATION_CHECKERS.forEach { useInstance(it) }
    DEFAULT_CLASH_RESOLVERS.forEach { useClashResolver(it) }
}


abstract class PlatformConfiguratorBase(
    private val dynamicTypesSettings: DynamicTypesSettings? = null,
    private val additionalDeclarationCheckers: List<DeclarationChecker> = emptyList(),
    private val additionalCallCheckers: List<CallChecker> = emptyList(),
    private val additionalAssignmentCheckers: List<AssignmentChecker> = emptyList(),
    private val additionalTypeCheckers: List<AdditionalTypeChecker> = emptyList(),
    private val additionalClassifierUsageCheckers: List<ClassifierUsageChecker> = emptyList(),
    private val additionalAnnotationCheckers: List<AdditionalAnnotationChecker> = emptyList(),
    private val additionalClashResolvers: List<PlatformExtensionsClashResolver<*>> = emptyList(),
    private val identifierChecker: IdentifierChecker? = null,
    private val overloadFilter: OverloadFilter? = null,
    private val platformToKotlinClassMapper: PlatformToKotlinClassMapper? = null,
    private val delegationFilter: DelegationFilter? = null,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper? = null,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer? = null
) : PlatformConfigurator {
    override val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        configureDefaultCheckers()
        configureExtensionsAndCheckers(this)
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        container.useImpl<OptInMarkerDeclarationAnnotationChecker>()
    }

    fun configureExtensionsAndCheckers(container: StorageComponentContainer) {
        with(container) {
            useInstanceIfNotNull(dynamicTypesSettings)
            additionalDeclarationCheckers.forEach { useInstance(it) }
            additionalCallCheckers.forEach { useInstance(it) }
            additionalAssignmentCheckers.forEach { useInstance(it) }
            additionalTypeCheckers.forEach { useInstance(it) }
            additionalClassifierUsageCheckers.forEach { useInstance(it) }
            additionalAnnotationCheckers.forEach { useInstance(it) }
            additionalClashResolvers.forEach { useClashResolver(it) }
            useInstanceIfNotNull(identifierChecker)
            useInstanceIfNotNull(overloadFilter)
            useInstanceIfNotNull(platformToKotlinClassMapper)
            useInstanceIfNotNull(delegationFilter)
            useInstanceIfNotNull(overridesBackwardCompatibilityHelper)
            useInstanceIfNotNull(declarationReturnTypeSanitizer)
        }
    }
}

fun createContainer(id: String, analyzerServices: PlatformDependentAnalyzerServices, init: StorageComponentContainer.() -> Unit) =
    composeContainer(id, analyzerServices.platformConfigurator.platformSpecificContainer, init)
