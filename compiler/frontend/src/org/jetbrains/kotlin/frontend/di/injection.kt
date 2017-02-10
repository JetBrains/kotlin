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

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.LazyResolveToken
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.*
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.types.expressions.DeclarationScopeProviderForLocalClassifierAnalyzer
import org.jetbrains.kotlin.types.expressions.LocalClassDescriptorHolder
import org.jetbrains.kotlin.types.expressions.LocalLazyDeclarationResolver

fun StorageComponentContainer.configureModule(
        moduleContext: ModuleContext, platform: TargetPlatform
) {
    useInstance(moduleContext)
    useInstance(moduleContext.module)
    useInstance(moduleContext.project)
    useInstance(moduleContext.storageManager)
    useInstance(moduleContext.module.builtIns)

    useInstance(platform)

    platform.platformConfigurator.configureModuleComponents(this)

    for (extension in StorageComponentContainerContributor.getInstances(moduleContext.project)) {
        extension.addDeclarations(this, platform)
    }

    configurePlatformIndependentComponents()
}

private fun StorageComponentContainer.configurePlatformIndependentComponents() {
    useImpl<SupertypeLoopCheckerImpl>()
}

fun StorageComponentContainer.configureModule(
        moduleContext: ModuleContext, platform: TargetPlatform, trace: BindingTrace
) {
    configureModule(moduleContext, platform)
    useInstance(trace)
}

fun StorageComponentContainer.configureCommon(configuration: CompilerConfiguration) {
    useInstance(configuration)
    useInstance(configuration.languageVersionSettings)
}

fun createContainerForBodyResolve(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        platform: TargetPlatform,
        statementFilter: StatementFilter,
        compilerConfiguration: CompilerConfiguration
): StorageComponentContainer = createContainer("BodyResolve", platform) {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(statementFilter)

    useInstance(LookupTracker.DO_NOTHING)
    useInstance(BodyResolveCache.ThrowException)
    configureCommon(compilerConfiguration)

    useImpl<BodyResolver>()
}

fun createContainerForLazyBodyResolve(
        moduleContext: ModuleContext,
        kotlinCodeAnalyzer: KotlinCodeAnalyzer,
        bindingTrace: BindingTrace,
        platform: TargetPlatform,
        bodyResolveCache: BodyResolveCache,
        compilerConfiguration: CompilerConfiguration
): StorageComponentContainer = createContainer("LazyBodyResolve", platform) {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(LookupTracker.DO_NOTHING)
    useInstance(kotlinCodeAnalyzer)
    useInstance(kotlinCodeAnalyzer.fileScopeProvider)
    useInstance(bodyResolveCache)
    configureCommon(compilerConfiguration)
    useImpl<LazyTopDownAnalyzer>()
    useImpl<BasicAbsentDescriptorHandler>()
}

fun createContainerForLazyLocalClassifierAnalyzer(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        platform: TargetPlatform,
        lookupTracker: LookupTracker,
        compilerConfiguration: CompilerConfiguration,
        statementFilter: StatementFilter,
        localClassDescriptorHolder: LocalClassDescriptorHolder
): StorageComponentContainer = createContainer("LocalClassifierAnalyzer", platform) {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(localClassDescriptorHolder)
    useInstance(lookupTracker)

    useImpl<LazyTopDownAnalyzer>()

    useInstance(NoTopLevelDescriptorProvider)

    CompilerEnvironment.configure(this)

    useInstance(FileScopeProvider.ThrowException)

    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()

    configureCommon(compilerConfiguration)
    useInstance(statementFilter)
}

fun createContainerForLazyResolve(
        moduleContext: ModuleContext,
        declarationProviderFactory: DeclarationProviderFactory,
        bindingTrace: BindingTrace,
        platform: TargetPlatform,
        targetEnvironment: TargetEnvironment,
        compilerConfiguration: CompilerConfiguration
): StorageComponentContainer = createContainer("LazyResolve", platform) {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(declarationProviderFactory)
    useInstance(LookupTracker.DO_NOTHING)

    configureCommon(compilerConfiguration)

    useImpl<FileScopeProviderImpl>()
    useImpl<CompilerDeserializationConfiguration>()
    targetEnvironment.configure(this)

    useImpl<LazyResolveToken>()
    useImpl<ResolveSession>()
}

fun createLazyResolveSession(moduleContext: ModuleContext, files: Collection<KtFile>): ResolveSession =
        createContainerForLazyResolve(
                moduleContext,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                BindingTraceContext(),
                TargetPlatform.Default,
                CompilerEnvironment,
                CompilerConfiguration.EMPTY
        ).get<ResolveSession>()
