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
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.LazyResolveToken
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
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
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.FileScopeProviderImpl
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
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
        targetEnvironment: TargetEnvironment,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
        useLazyResolve: Boolean
): StorageComponentContainer = createContainer("LazyResolveWithJava", JvmPlatform) {
    configureModule(moduleContext, JvmPlatform, bindingTrace)
    configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, lookupTracker, languageVersionSettings)

    useInstance(packagePartProvider)
    useInstance(moduleClassResolver)
    useInstance(declarationProviderFactory)

    targetEnvironment.configure(this)

    if (useLazyResolve) {
        useImpl<LazyResolveToken>()
    }
}.apply {
    get<JavaClassFinderImpl>().initialize(bindingTrace, get<KotlinCodeAnalyzer>())
}


fun createContainerForTopDownAnalyzerForJvm(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings,
        moduleClassResolver: ModuleClassResolver
): ComponentProvider = createContainerForLazyResolveWithJava(
        moduleContext, bindingTrace, declarationProviderFactory, moduleContentScope, moduleClassResolver,
        CompilerEnvironment, lookupTracker, packagePartProvider, languageVersionSettings, useLazyResolve = false
)


fun createContainerForTopDownSingleModuleAnalyzerForJvm(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        packagePartProvider: PackagePartProvider,
        languageVersionSettings: LanguageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
): ComponentProvider = createContainerForTopDownAnalyzerForJvm(
        moduleContext, bindingTrace, declarationProviderFactory, moduleContentScope,
        LookupTracker.DO_NOTHING, packagePartProvider, languageVersionSettings, SingleModuleClassResolver()
).apply {
    initJvmBuiltInsForTopDownAnalysis(moduleContext.module, languageVersionSettings)
    get<SingleModuleClassResolver>().resolver = get<JavaDescriptorResolver>()
}


fun ComponentProvider.initJvmBuiltInsForTopDownAnalysis(module: ModuleDescriptor, languageVersionSettings: LanguageVersionSettings) {
    get<JvmBuiltIns>().initialize(module, languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
}
