/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.composeContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.PlatformToKotlinClassMap
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.checkers.*
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticConstructorsProvider
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesSettings
import java.util.*

abstract class TargetPlatform(val platformName: String) {
    override fun toString() = platformName

    abstract val platformConfigurator: PlatformConfigurator
    abstract fun getDefaultImports(languageVersionSettings: LanguageVersionSettings): List<ImportPath>
    open val excludedImports: List<FqName> get() = emptyList()

    abstract val multiTargetPlatform: MultiTargetPlatform

    object Default : TargetPlatform("Default") {
        override fun getDefaultImports(languageVersionSettings: LanguageVersionSettings): List<ImportPath> = ArrayList<ImportPath>().apply {
            add(ImportPath("kotlin.*"))
            add(ImportPath("kotlin.annotation.*"))
            add(ImportPath("kotlin.collections.*"))
            add(ImportPath("kotlin.ranges.*"))
            add(ImportPath("kotlin.sequences.*"))
            add(ImportPath("kotlin.text.*"))
            add(ImportPath("kotlin.io.*"))

            if (languageVersionSettings.supportsFeature(LanguageFeature.DefaultImportOfPackageKotlinComparisons)) {
                add(ImportPath("kotlin.comparisons.*"))
            }
        }

        override val platformConfigurator =
                object : PlatformConfigurator(
                        DynamicTypesSettings(), listOf(), listOf(), listOf(), listOf(), listOf(),
                        IdentifierChecker.DEFAULT, OverloadFilter.DEFAULT, PlatformToKotlinClassMap.EMPTY, DelegationFilter.DEFAULT,
                        OverridesBackwardCompatibilityHelper.DEFAULT
                ) {
                    override fun configureModuleComponents(container: StorageComponentContainer) {
                        container.useInstance(SyntheticScopes.Empty)
                        container.useInstance(SyntheticConstructorsProvider.Empty)
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
        ReifiedTypeParameterAnnotationChecker(),
        DynamicReceiverChecker,
        DelegationChecker(),
        KClassWithIncorrectTypeArgumentChecker,
        SuspendOperatorsCheckers
)

private val DEFAULT_CALL_CHECKERS = listOf(
        CapturingInClosureChecker(), InlineCheckerWrapper(), SafeCallChecker(),
        DeprecatedCallChecker, CallReturnsArrayOfNothingChecker(), InfixCallChecker(), OperatorCallChecker(),
        ConstructorHeaderCallChecker, ProtectedConstructorCallChecker, ApiVersionCallChecker,
        CoroutineSuspendCallChecker, BuilderFunctionsCallChecker, DslScopeViolationCallChecker, MissingDependencyClassChecker,
        CallableReferenceCompatibilityChecker()
)
private val DEFAULT_TYPE_CHECKERS = emptyList<AdditionalTypeChecker>()
private val DEFAULT_CLASSIFIER_USAGE_CHECKERS = listOf(
        DeprecatedClassifierUsageChecker(), ApiVersionClassifierUsageChecker, MissingDependencyClassChecker.ClassifierUsage
)


abstract class PlatformConfigurator(
        private val dynamicTypesSettings: DynamicTypesSettings,
        additionalDeclarationCheckers: List<DeclarationChecker>,
        additionalCallCheckers: List<CallChecker>,
        additionalTypeCheckers: List<AdditionalTypeChecker>,
        additionalClassifierUsageCheckers: List<ClassifierUsageChecker>,
        private val additionalAnnotationCheckers: List<AdditionalAnnotationChecker>,
        private val identifierChecker: IdentifierChecker,
        private val overloadFilter: OverloadFilter,
        private val platformToKotlinClassMap: PlatformToKotlinClassMap,
        private val delegationFilter: DelegationFilter,
        private val overridesBackwardCompatibilityHelper: OverridesBackwardCompatibilityHelper
) {
    private val declarationCheckers: List<DeclarationChecker> = DEFAULT_DECLARATION_CHECKERS + additionalDeclarationCheckers
    private val callCheckers: List<CallChecker> = DEFAULT_CALL_CHECKERS + additionalCallCheckers
    private val typeCheckers: List<AdditionalTypeChecker> = DEFAULT_TYPE_CHECKERS + additionalTypeCheckers
    private val classifierUsageCheckers: List<ClassifierUsageChecker> = DEFAULT_CLASSIFIER_USAGE_CHECKERS + additionalClassifierUsageCheckers

    abstract fun configureModuleComponents(container: StorageComponentContainer)

    val platformSpecificContainer = composeContainer(this.javaClass.simpleName) {
        useInstance(dynamicTypesSettings)
        declarationCheckers.forEach { useInstance(it) }
        callCheckers.forEach { useInstance(it) }
        typeCheckers.forEach { useInstance(it) }
        classifierUsageCheckers.forEach { useInstance(it) }
        additionalAnnotationCheckers.forEach { useInstance(it) }
        useInstance(identifierChecker)
        useInstance(overloadFilter)
        useInstance(platformToKotlinClassMap)
        useInstance(delegationFilter)
        useInstance(overridesBackwardCompatibilityHelper)
    }
}

fun createContainer(id: String, platform: TargetPlatform, init: StorageComponentContainer.() -> Unit)
        = composeContainer(id, platform.platformConfigurator.platformSpecificContainer, init)
