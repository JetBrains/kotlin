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

package org.jetbrains.kotlin.cli.jvm.repl.di

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.java.di.configureJavaTopDownAnalysis
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.BodyResolveCache
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.jvm.JavaClassFinderPostConstruct
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.FileScopeProvider
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

public fun createContainerForReplWithJava(
        moduleContext: ModuleContext, bindingTrace: BindingTrace, declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope, additionalFileScopeProvider: FileScopeProvider.AdditionalScopes
): ContainerForReplWithJava = createContainer("ReplWithJava") {
    configureModule(moduleContext, JvmPlatform, bindingTrace)
    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, LookupTracker.DO_NOTHING)

    useInstance(additionalFileScopeProvider)
    useInstance(declarationProviderFactory)
    useInstance(BodyResolveCache.ThrowException)

    useImpl<FileScopeProviderImpl>()
    useImpl<SingleModuleClassResolver>()
}.let {
    it.get<JavaClassFinderImpl>().initialize()
    it.get<JavaClassFinderPostConstruct>().postCreate()
    ContainerForReplWithJava(it)
}

public class ContainerForReplWithJava(container: StorageComponentContainer) {
    val resolveSession: ResolveSession by container
    val lazyTopDownAnalyzerForTopLevel: LazyTopDownAnalyzerForTopLevel by container
    val javaDescriptorResolver: JavaDescriptorResolver by container
}
