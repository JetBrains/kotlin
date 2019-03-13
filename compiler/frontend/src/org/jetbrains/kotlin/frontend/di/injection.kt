/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.frontend.di

import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValueFactoryImpl
import org.jetbrains.kotlin.resolve.calls.tower.KotlinResolutionStatelessCallbacksImpl
import org.jetbrains.kotlin.resolve.checkers.ExperimentalUsageChecker
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import org.jetbrains.kotlin.types.expressions.LocalClassDescriptorHolder
import org.jetbrains.kotlin.types.expressions.LocalLazyDeclarationResolver

fun StorageComponentContainer.configureModule(
    moduleContext: ModuleContext,
    platform: TargetPlatform,
    compilerServices: PlatformDependentCompilerServices,
    trace: BindingTrace
) {
    useInstance(moduleContext)
    useInstance(moduleContext.module)
    useInstance(moduleContext.project)
    useInstance(moduleContext.storageManager)
    useInstance(moduleContext.module.builtIns)
    useInstance(trace)

    useInstance(platform)
    useInstance(compilerServices)
    platform.subplatformOfType<JdkPlatform>()?.targetVersion?.let { useInstance(it) }

    compilerServices.platformConfigurator.configureModuleComponents(this)
    compilerServices.platformConfigurator.configureModuleDependentCheckers(this)

    for (extension in StorageComponentContainerContributor.getInstances(moduleContext.project)) {
        extension.registerModuleComponents(this, platform, moduleContext.module)
    }

    configurePlatformIndependentComponents()
}

private fun StorageComponentContainer.configurePlatformIndependentComponents() {
    useImpl<SupertypeLoopCheckerImpl>()
    useImpl<KotlinResolutionStatelessCallbacksImpl>()
    useImpl<DataFlowValueFactoryImpl>()

    useImpl<ExperimentalUsageChecker>()
    useImpl<ExperimentalUsageChecker.Overrides>()
    useImpl<ExperimentalUsageChecker.ClassifierUsage>()
}

fun createContainerForBodyResolve(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    statementFilter: StatementFilter,
    compilerServices: PlatformDependentCompilerServices,
    languageVersionSettings: LanguageVersionSettings
): StorageComponentContainer = createContainer("BodyResolve", compilerServices) {
    configureModule(moduleContext, platform, compilerServices, bindingTrace)

    useInstance(statementFilter)

    useInstance(BodyResolveCache.ThrowException)
    useInstance(languageVersionSettings)
    useImpl<AnnotationResolverImpl>()

    useImpl<BodyResolver>()
}

fun createContainerForLazyBodyResolve(
    moduleContext: ModuleContext,
    kotlinCodeAnalyzer: KotlinCodeAnalyzer,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    bodyResolveCache: BodyResolveCache,
    compilerServices: PlatformDependentCompilerServices,
    languageVersionSettings: LanguageVersionSettings
): StorageComponentContainer = createContainer("LazyBodyResolve", compilerServices) {
    configureModule(moduleContext, platform, compilerServices, bindingTrace)

    useInstance(kotlinCodeAnalyzer)
    useInstance(kotlinCodeAnalyzer.fileScopeProvider)
    useInstance(bodyResolveCache)
    useInstance(languageVersionSettings)
    useImpl<AnnotationResolverImpl>()
    useImpl<LazyTopDownAnalyzer>()
    useImpl<BasicAbsentDescriptorHandler>()
}

fun createContainerForLazyLocalClassifierAnalyzer(
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    lookupTracker: LookupTracker,
    languageVersionSettings: LanguageVersionSettings,
    statementFilter: StatementFilter,
    localClassDescriptorHolder: LocalClassDescriptorHolder,
    compilerServices: PlatformDependentCompilerServices
): StorageComponentContainer = createContainer("LocalClassifierAnalyzer", compilerServices) {
    configureModule(moduleContext, platform, compilerServices, bindingTrace)

    useInstance(localClassDescriptorHolder)
    useInstance(lookupTracker)
    useInstance(ExpectActualTracker.DoNothing)

    useImpl<LazyTopDownAnalyzer>()

    useInstance(NoTopLevelDescriptorProvider)

    CompilerEnvironment.configure(this)

    useInstance(FileScopeProvider.ThrowException)
    useImpl<AnnotationResolverImpl>()

    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()

    useInstance(languageVersionSettings)
    useInstance(statementFilter)
}

fun createContainerForLazyResolve(
    moduleContext: ModuleContext,
    declarationProviderFactory: DeclarationProviderFactory,
    bindingTrace: BindingTrace,
    platform: TargetPlatform,
    compilerServices: PlatformDependentCompilerServices,
    targetEnvironment: TargetEnvironment,
    languageVersionSettings: LanguageVersionSettings
): StorageComponentContainer = createContainer("LazyResolve", compilerServices) {
    configureModule(moduleContext, platform, compilerServices, bindingTrace)

    useInstance(declarationProviderFactory)
    useInstance(languageVersionSettings)

    useImpl<AnnotationResolverImpl>()
    useImpl<CompilerDeserializationConfiguration>()
    targetEnvironment.configure(this)

    useImpl<ResolveSession>()
    useImpl<LazyTopDownAnalyzer>()
}