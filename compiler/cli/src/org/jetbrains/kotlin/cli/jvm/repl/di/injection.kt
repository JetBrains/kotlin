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

package org.jetbrains.kotlin.cli.jvm.repl.di

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.getValue
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.java.di.configureJavaTopDownAnalysis
import org.jetbrains.kotlin.frontend.java.di.initJvmBuiltInsForTopDownAnalysis
import org.jetbrains.kotlin.frontend.java.di.javaAnalysisInit
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.createContainer
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun createContainerForReplWithJava(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        packagePartProvider: PackagePartProvider
): ContainerForReplWithJava = createContainer("ReplWithJava", JvmPlatform) {
    useInstance(packagePartProvider)
    configureModule(moduleContext, JvmPlatform, bindingTrace)
    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, LookupTracker.DO_NOTHING, LanguageVersionSettingsImpl.DEFAULT)

    useInstance(declarationProviderFactory)

    CompilerEnvironment.configure(this)

    useImpl<SingleModuleClassResolver>()
}.let {
    it.javaAnalysisInit()
    it.initJvmBuiltInsForTopDownAnalysis()

    ContainerForReplWithJava(it)
}

class ContainerForReplWithJava(container: StorageComponentContainer) {
    val resolveSession: ResolveSession by container
    val lazyTopDownAnalyzerForTopLevel: LazyTopDownAnalyzerForTopLevel by container
    val javaDescriptorResolver: JavaDescriptorResolver by container
}
