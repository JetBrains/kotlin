/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.composeContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.types.DynamicTypesSettings
import java.util.*

abstract class TargetPlatform(val platformName: String) {
    private data class DefaultImportsKey(val includeKotlinComparisons: Boolean, val includeLowPriorityImports: Boolean)

    private val defaultImports = LockBasedStorageManager().let { storageManager ->
        storageManager.createMemoizedFunction<DefaultImportsKey, List<ImportPath>> { (includeKotlinComparisons, includeLowPriorityImports) ->
            ArrayList<ImportPath>().apply {
                listOf(
                    "kotlin.*",
                    "kotlin.annotation.*",
                    "kotlin.collections.*",
                    "kotlin.ranges.*",
                    "kotlin.sequences.*",
                    "kotlin.text.*",
                    "kotlin.io.*"
                ).forEach { add(ImportPath.fromString(it)) }

                if (includeKotlinComparisons) {
                    add(ImportPath.fromString("kotlin.comparisons.*"))
                }

                computePlatformSpecificDefaultImports(storageManager, this)

                if (includeLowPriorityImports) {
                    addAll(defaultLowPriorityImports)
                }
            }
        }
    }

    override fun toString() = platformName

    abstract val platformConfigurator: PlatformConfigurator

    open val defaultLowPriorityImports: List<ImportPath> get() = emptyList()

    fun getDefaultImports(languageVersionSettings: LanguageVersionSettings, includeLowPriorityImports: Boolean): List<ImportPath> =
        defaultImports(
            DefaultImportsKey(
                languageVersionSettings.supportsFeature(LanguageFeature.DefaultImportOfPackageKotlinComparisons),
                includeLowPriorityImports
            )
        )

    protected abstract fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>)

    open val excludedImports: List<FqName> get() = emptyList()

    abstract val multiTargetPlatform: MultiTargetPlatform

    // This function is used in "cat.helm.clean:0.1.1-SNAPSHOT": https://plugins.jetbrains.com/plugin/index?xmlId=cat.helm.clean
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Use getDefaultImports(LanguageVersionSettings, Boolean) instead.", level = DeprecationLevel.ERROR)
    fun getDefaultImports(includeKotlinComparisons: Boolean): List<ImportPath> {
        return getDefaultImports(
            if (includeKotlinComparisons) LanguageVersionSettingsImpl.DEFAULT
            else LanguageVersionSettingsImpl(LanguageVersion.KOTLIN_1_0, ApiVersion.KOTLIN_1_0),
            true
        )
    }

    object Common : TargetPlatform("Default") {
        override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {}

        override val platformConfigurator =
            object : PlatformConfigurator(
                DynamicTypesSettings(), listOf(), listOf(), listOf(), listOf(), listOf(),
                IdentifierChecker.Default, OverloadFilter.Default, PlatformToKotlinClassMap.EMPTY, DelegationFilter.Default,
                OverridesBackwardCompatibilityHelper.Default,
                DeclarationReturnTypeSanitizer.Default
            ) {
                override fun configureModuleComponents(container: StorageComponentContainer) {
                    container.useInstance(SyntheticScopes.Empty)
                    container.useInstance(SamConversionTransformer.Empty)
                    container.useInstance(TypeSpecificityComparator.NONE)
                }
            }

        override val multiTargetPlatform: MultiTargetPlatform
            get() = MultiTargetPlatform.Common
    }
}

private val DEFAULT_DECLARATION_CHECKERS = listOf(
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
    InlineClassDeclarationChecker,
    PropertiesWithBackingFieldsInsideInlineClass(),
    AnnotationClassTargetAndRetentionChecker(),
    ReservedMembersAndConstructsForInlineClass(),
    ResultClassInReturnTypeChecker()
)

private val DEFAULT_CALL_CHECKERS = listOf(
    CapturingInClosureChecker(), InlineCheckerWrapper(), SafeCallChecker(),
    DeprecatedCallChecker, CallReturnsArrayOfNothingChecker(), InfixCallChecker(), OperatorCallChecker(),
    ConstructorHeaderCallChecker, ProtectedConstructorCallChecker, ApiVersionCallChecker,
    CoroutineSuspendCallChecker, BuilderFunctionsCallChecker, DslScopeViolationCallChecker, MissingDependencyClassChecker,
    CallableReferenceCompatibilityChecker(), LateinitIntrinsicApplicabilityChecker,
    UnderscoreUsageChecker, AssigningNamedArgumentToVarargChecker(),
    PrimitiveNumericComparisonCallChecker, LambdaWithSuspendModifierCallChecker,
    UselessElvisCallChecker(), ResultTypeWithNullableOperatorsChecker()
)
private val DEFAULT_TYPE_CHECKERS = emptyList<AdditionalTypeChecker>()
private val DEFAULT_CLASSIFIER_USAGE_CHECKERS = listOf(
    DeprecatedClassifierUsageChecker(), ApiVersionClassifierUsageChecker, MissingDependencyClassChecker.ClassifierUsage,
    OptionalExpectationUsageChecker()
)
private val DEFAULT_ANNOTATION_CHECKERS = listOf<AdditionalAnnotationChecker>()


abstract class PlatformConfigurator(
    private val dynamicTypesSettings: DynamicTypesSettings,
    additionalDeclarationCheckers: List<DeclarationChecker>,
    additionalCallCheckers: List<CallChecker>,
    additionalTypeCheckers: List<AdditionalTypeChecker>,
    additionalClassifierUsageCheckers: List<ClassifierUsageChecker>,
    additionalAnnotationCheckers: List<AdditionalAnnotationChecker>,
    private val identifierChecker: IdentifierChecker,
    private val overloadFilter: OverloadFilter,
    private val platformToKotlinClassMap: PlatformToKotlinClassMap,
    private val delegationFilter: DelegationFilter,
    private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper,
    private val declarationReturnTypeSanitizer: DeclarationReturnTypeSanitizer
) {
    private val declarationCheckers: List<DeclarationChecker> = DEFAULT_DECLARATION_CHECKERS + additionalDeclarationCheckers
    private val callCheckers: List<CallChecker> = DEFAULT_CALL_CHECKERS + additionalCallCheckers
    private val typeCheckers: List<AdditionalTypeChecker> = DEFAULT_TYPE_CHECKERS + additionalTypeCheckers
    private val classifierUsageCheckers: List<ClassifierUsageChecker> =
        DEFAULT_CLASSIFIER_USAGE_CHECKERS + additionalClassifierUsageCheckers
    private val annotationCheckers: List<AdditionalAnnotationChecker> = DEFAULT_ANNOTATION_CHECKERS + additionalAnnotationCheckers

    abstract fun configureModuleComponents(container: StorageComponentContainer)

    val platformSpecificContainer = composeContainer(this::class.java.simpleName) {
        useInstance(dynamicTypesSettings)
        declarationCheckers.forEach { useInstance(it) }
        callCheckers.forEach { useInstance(it) }
        typeCheckers.forEach { useInstance(it) }
        classifierUsageCheckers.forEach { useInstance(it) }
        annotationCheckers.forEach { useInstance(it) }
        useInstance(identifierChecker)
        useInstance(overloadFilter)
        useInstance(platformToKotlinClassMap)
        useInstance(delegationFilter)
        useInstance(overridesBackwardCompatibilityHelper)
        useInstance(declarationReturnTypeSanitizer)
    }

    fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        container.useImpl<ExperimentalMarkerDeclarationAnnotationChecker>()
    }
}

fun createContainer(id: String, platform: TargetPlatform, init: StorageComponentContainer.() -> Unit) =
    composeContainer(id, platform.platformConfigurator.platformSpecificContainer, init)
