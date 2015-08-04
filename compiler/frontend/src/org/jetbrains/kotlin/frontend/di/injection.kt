/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.LazyResolveToken
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.NoTopLevelDescriptorProvider
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.types.expressions.*

public fun StorageComponentContainer.configureModule(
        moduleContext: ModuleContext, platform: TargetPlatform
) {
    useInstance(moduleContext)
    useInstance(moduleContext.module)
    useInstance(moduleContext.project)
    useInstance(moduleContext.storageManager)
    useInstance(moduleContext.builtIns)
    useInstance(moduleContext.platformToKotlinClassMap)

    useInstance(platform)

    platform.platformConfigurator.configure(this)
}

public fun StorageComponentContainer.configureModule(
        moduleContext: ModuleContext, platform: TargetPlatform, trace: BindingTrace
) {
    configureModule(moduleContext, platform)
    useInstance(trace)
}

public fun createContainerForBodyResolve(
        moduleContext: ModuleContext, bindingTrace: BindingTrace,
        platform: TargetPlatform, statementFilter: StatementFilter
): StorageComponentContainer = createContainer("BodyResolve") {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(statementFilter)
    useInstance(BodyResolveCache.ThrowException)
    useImpl<BodyResolver>()
}

public fun createContainerForLazyBodyResolve(
        moduleContext: ModuleContext, kotlinCodeAnalyzer: KotlinCodeAnalyzer,
        bindingTrace: BindingTrace, platform: TargetPlatform,
        bodyResolveCache: BodyResolveCache
): StorageComponentContainer = createContainer("LazyBodyResolve") {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(kotlinCodeAnalyzer)
    useInstance(kotlinCodeAnalyzer.getFileScopeProvider())
    useInstance(bodyResolveCache)
    useImpl<LazyTopDownAnalyzerForTopLevel>()
}

public fun createContainerForLazyLocalClassifierAnalyzer(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        platform: TargetPlatform,
        localClassDescriptorHolder: LocalClassDescriptorHolder
): StorageComponentContainer = createContainer("LocalClassifierAnalyzer") {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(localClassDescriptorHolder)

    useImpl<LazyTopDownAnalyzer>()

    useInstance(NoTopLevelDescriptorProvider)
    useInstance(BodyResolveCache.ThrowException)
    useInstance(FileScopeProvider.ThrowException)

    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()
}

private fun createContainerForLazyResolve(
        moduleContext: ModuleContext,
        declarationProviderFactory: DeclarationProviderFactory,
        bindingTrace: BindingTrace,
        platform: TargetPlatform
): StorageComponentContainer = createContainer("LazyResolve") {
    configureModule(moduleContext, platform, bindingTrace)

    useInstance(declarationProviderFactory)
    useInstance(LookupTracker.DO_NOTHING)

    useImpl<LazyResolveToken>()
    useImpl<ResolveSession>()
}

public fun createLazyResolveSession(
        moduleContext: ModuleContext, declarationProviderFactory: DeclarationProviderFactory, bindingTrace: BindingTrace,
        platform: TargetPlatform
): ResolveSession = createContainerForLazyResolve(moduleContext, declarationProviderFactory, bindingTrace, platform).get<ResolveSession>()

public fun createContainerForMacros(project: Project, module: ModuleDescriptor): ContainerForMacros {
    val componentContainer = createContainer("Macros") {
        configureModule(ModuleContext(module, project), TargetPlatform.Default)
        useImpl<ExpressionTypingServices>()
    }
    return ContainerForMacros(componentContainer)
}

public class ContainerForMacros(container: StorageComponentContainer) {
    val callResolver: CallResolver by container
    val typeResolver: TypeResolver by container
    val expressionTypingComponents: ExpressionTypingComponents by container
    val expressionTypingServices: ExpressionTypingServices by container
}
