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

package org.jetbrains.kotlin.frontend.java.di

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.LazyResolveToken
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.java.components.*
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.lazy.SingleModuleClassResolver
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.JvmVirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.JavaClassFinderPostConstruct
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JavaLazyAnalyzerPostConstruct
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun StorageComponentContainer.configureJavaTopDownAnalysis(
        moduleContentScope: GlobalSearchScope,
        project: Project,
        lookupTracker: LookupTracker,
        languageVersionSettings: LanguageVersionSettings
) {
    useInstance(moduleContentScope)
    useInstance(lookupTracker)
    useImpl<ResolveSession>()

    useImpl<LazyTopDownAnalyzer>()
    useImpl<LazyTopDownAnalyzerForTopLevel>()
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()

    useInstance(JvmVirtualFileFinderFactory.SERVICE.getInstance(project).create(moduleContentScope))

    useImpl<FileScopeProviderImpl>()

    useImpl<JavaClassFinderImpl>()
    useImpl<SignaturePropagatorImpl>()
    useImpl<LazyResolveBasedCache>()
    useImpl<TraceBasedErrorReporter>()
    useImpl<PsiBasedExternalAnnotationResolver>()
    useImpl<JavaPropertyInitializerEvaluatorImpl>()
    useInstance(SamConversionResolverImpl)
    useImpl<JavaSourceElementFactoryImpl>()
    useImpl<JavaLazyAnalyzerPostConstruct>()
    useInstance(InternalFlexibleTypeTransformer)

    useInstance(languageVersionSettings)
    useImpl<CompilerDeserializationConfiguration>()
}

fun createContainerForLazyResolveWithJava(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        moduleClassResolver: ModuleClassResolver,
        targetEnvironment: TargetEnvironment = CompilerEnvironment,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings
): ComponentProvider = createContainer("LazyResolveWithJava") {
    //TODO: idea specific code
    useInstance(packagePartProvider)

    configureModule(moduleContext, JvmPlatform, bindingTrace)

    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, LookupTracker.DO_NOTHING, languageVersionSettings)

    useInstance(moduleClassResolver)

    useInstance(declarationProviderFactory)

    targetEnvironment.configure(this)

    useImpl<LazyResolveToken>()
}.apply {
    javaAnalysisInit()
}


fun createContainerForTopDownAnalyzerForJvm(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings
): ContainerForTopDownAnalyzerForJvm = createContainer("TopDownAnalyzerForJvm") {
    useInstance(packagePartProvider)

    configureModule(moduleContext, JvmPlatform, bindingTrace)
    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, lookupTracker, languageVersionSettings)

    useInstance(declarationProviderFactory)

    CompilerEnvironment.configure(this)

    useImpl<SingleModuleClassResolver>()
}.let {
    it.javaAnalysisInit()

    ContainerForTopDownAnalyzerForJvm(it)
}

private fun StorageComponentContainer.javaAnalysisInit() {
    get<JavaClassFinderImpl>().initialize()
    get<JavaClassFinderPostConstruct>().postCreate()
}

class ContainerForTopDownAnalyzerForJvm(val container: StorageComponentContainer) {
    val lazyTopDownAnalyzerForTopLevel: LazyTopDownAnalyzerForTopLevel by container
    val javaDescriptorResolver: JavaDescriptorResolver by container
    val deserializationComponentsForJava: DeserializationComponentsForJava by container
}
