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
import org.jetbrains.kotlin.incremental.components.UsageCollector
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.CallResolver
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.NoTopLevelDescriptorProvider
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.types.DynamicTypesSettings
import org.jetbrains.kotlin.types.expressions.*

public fun StorageComponentContainer.configureModule(
        moduleContext: ModuleContext, additionalCheckerProvider: AdditionalCheckerProvider
) {
    useInstance(moduleContext)
    useInstance(moduleContext.module)
    useInstance(moduleContext.project)
    useInstance(moduleContext.storageManager)
    useInstance(moduleContext.builtIns)
    useInstance(moduleContext.platformToKotlinClassMap)
    useInstance(additionalCheckerProvider)
    useInstance(additionalCheckerProvider.symbolUsageValidator)

    additionalCheckerProvider.declarationCheckers.forEach {
        useInstance(it)
    }
    additionalCheckerProvider.typeCheckers.forEach {
        useInstance(it)
    }
}

public fun StorageComponentContainer.configureModule(
        moduleContext: ModuleContext, additionalCheckerProvider: AdditionalCheckerProvider, trace: BindingTrace
) {
    configureModule(moduleContext, additionalCheckerProvider)
    useInstance(trace)
}

public fun createContainerForBodyResolve(
        moduleContext: ModuleContext, bindingTrace: BindingTrace,
        additionalCheckerProvider: AdditionalCheckerProvider, statementFilter: StatementFilter,
        dynamicTypesSettings: DynamicTypesSettings
): StorageComponentContainer = createContainer("BodyResolve") {
    configureModule(moduleContext, additionalCheckerProvider, bindingTrace)

    useInstance(statementFilter)
    useInstance(dynamicTypesSettings)
    useInstance(BodyResolveCache.ThrowException)
    useImpl<BodyResolver>()
}

public fun createContainerForLazyBodyResolve(
        moduleContext: ModuleContext, kotlinCodeAnalyzer: KotlinCodeAnalyzer,
        bindingTrace: BindingTrace, additionalCheckerProvider: AdditionalCheckerProvider,
        dynamicTypesSettings: DynamicTypesSettings,
        bodyResolveCache: BodyResolveCache
): StorageComponentContainer = createContainer("LazyBodyResolve") {
    configureModule(moduleContext, additionalCheckerProvider, bindingTrace)

    useInstance(kotlinCodeAnalyzer)
    useInstance(kotlinCodeAnalyzer.getFileScopeProvider())
    useInstance(dynamicTypesSettings)
    useInstance(bodyResolveCache)
    useImpl<LazyTopDownAnalyzerForTopLevel>()
}

public fun createContainerForLazyLocalClassifierAnalyzer(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        additionalCheckerProvider: AdditionalCheckerProvider,
        dynamicTypesSettings: DynamicTypesSettings,
        localClassDescriptorHolder: LocalClassDescriptorHolder
): StorageComponentContainer = createContainer("LocalClassifierAnalyzer") {
    configureModule(moduleContext, additionalCheckerProvider, bindingTrace)

    useInstance(dynamicTypesSettings)
    useInstance(localClassDescriptorHolder)

    useImpl<LazyTopDownAnalyzer>()

    useInstance(NoTopLevelDescriptorProvider)
    useInstance(BodyResolveCache.ThrowException)
    useInstance(FileScopeProvider.ThrowException)

    useImpl<DeclarationScopeProviderForLocalClassifierAnalyzer>()
    useImpl<LocalLazyDeclarationResolver>()
}

private fun createContainerForLazyResolve(
        moduleContext: ModuleContext, declarationProviderFactory: DeclarationProviderFactory, bindingTrace: BindingTrace,
        additionalCheckerProvider: AdditionalCheckerProvider, dynamicTypesSettings: DynamicTypesSettings
): StorageComponentContainer = createContainer("LazyResolve") {
    configureModule(moduleContext, additionalCheckerProvider, bindingTrace)

    useInstance(dynamicTypesSettings)
    useInstance(declarationProviderFactory)
    useInstance(UsageCollector.DO_NOTHING)

    useImpl<LazyResolveToken>()
    useImpl<ResolveSession>()
}

public fun createLazyResolveSession(
        moduleContext: ModuleContext, declarationProviderFactory: DeclarationProviderFactory, bindingTrace: BindingTrace,
        additionalCheckerProvider: AdditionalCheckerProvider, dynamicTypesSettings: DynamicTypesSettings
): ResolveSession = createContainerForLazyResolve(
        moduleContext, declarationProviderFactory, bindingTrace, additionalCheckerProvider, dynamicTypesSettings
).get<ResolveSession>()

public fun createContainerForMacros(project: Project, module: ModuleDescriptor): ContainerForMacros {
    val componentContainer = createContainer("Macros") {
        configureModule(ModuleContext(module, project), AdditionalCheckerProvider.DefaultProvider)
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
