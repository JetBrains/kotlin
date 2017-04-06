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
import org.jetbrains.kotlin.builtins.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.components.JavacBasedClassFinder
import org.jetbrains.kotlin.components.JavacBasedJavaResolverCache
import org.jetbrains.kotlin.components.JavacBasedSourceElementFactory
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
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
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.load.java.sam.SamWithReceiverResolver
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

private fun StorageComponentContainer.configureJavaTopDownAnalysisWithJavac(
        moduleContentScope: GlobalSearchScope,
        project: Project,
        lookupTracker: LookupTracker
) {
    useInstance(moduleContentScope)
    useInstance(lookupTracker)
    useImpl<ResolveSession>()

    useImpl<LazyTopDownAnalyzer>()
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()

    useInstance(VirtualFileFinderFactory.getInstance(project).create(moduleContentScope))

    useImpl<AnnotationResolverImpl>()

    useImpl<JavacBasedClassFinder>()
    useImpl<SignaturePropagatorImpl>()
    useImpl<JavacBasedJavaResolverCache>()
    useImpl<TraceBasedErrorReporter>()
    useImpl<PsiBasedExternalAnnotationResolver>()
    useInstance(JavaPropertyInitializerEvaluator.DoNothing)
    useInstance(SamWithReceiverResolver())
    useImpl<SamConversionResolverImpl>()
    useImpl<JavacBasedSourceElementFactory>()
    useInstance(InternalFlexibleTypeTransformer)

    useImpl<CompilerDeserializationConfiguration>()
}

private fun StorageComponentContainer.configureJavaTopDownAnalysis(
        moduleContentScope: GlobalSearchScope,
        project: Project,
        lookupTracker: LookupTracker
) {
    useInstance(moduleContentScope)
    useInstance(lookupTracker)
    useImpl<ResolveSession>()

    useImpl<LazyTopDownAnalyzer>()
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()

    useInstance(VirtualFileFinderFactory.getInstance(project).create(moduleContentScope))

    useImpl<AnnotationResolverImpl>()

    useImpl<JavaClassFinderImpl>()
    useImpl<SignaturePropagatorImpl>()
    useImpl<LazyResolveBasedCache>()
    useImpl<TraceBasedErrorReporter>()
    useImpl<PsiBasedExternalAnnotationResolver>()
    useImpl<JavaPropertyInitializerEvaluatorImpl>()
    useInstance(SamWithReceiverResolver())
    useImpl<SamConversionResolverImpl>()
    useImpl<JavaSourceElementFactoryImpl>()
    useInstance(InternalFlexibleTypeTransformer)

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
        jvmTarget: JvmTarget,
        languageVersionSettings: LanguageVersionSettings,
        useBuiltInsProvider: Boolean,
        useLazyResolve: Boolean,
        useJavac: Boolean
): StorageComponentContainer = createContainer("LazyResolveWithJava", JvmPlatform) {
    configureModule(moduleContext, JvmPlatform, jvmTarget, bindingTrace)
    if (useJavac)
        configureJavaTopDownAnalysisWithJavac(moduleContentScope, moduleContext.project, lookupTracker)
    else
        configureJavaTopDownAnalysis(moduleContentScope, moduleContext.project, lookupTracker)

    useInstance(packagePartProvider)
    useInstance(moduleClassResolver)
    useInstance(declarationProviderFactory)

    useInstance(languageVersionSettings)

    if (useBuiltInsProvider) {
        useInstance((moduleContext.module.builtIns as JvmBuiltIns).settings)
        useImpl<JvmBuiltInsPackageFragmentProvider>()
    }

    targetEnvironment.configure(this)

    if (useLazyResolve) {
        useImpl<LazyResolveToken>()
    }
}.apply {
    if (useJavac)
        get<JavacBasedClassFinder>().initialize(bindingTrace, get<KotlinCodeAnalyzer>())
    else
        get<JavaClassFinderImpl>().initialize(bindingTrace, get<KotlinCodeAnalyzer>())
}


fun createContainerForTopDownAnalyzerForJvm(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        moduleContentScope: GlobalSearchScope,
        lookupTracker: LookupTracker,
        packagePartProvider: PackagePartProvider,
        moduleClassResolver: ModuleClassResolver,
        jvmTarget: JvmTarget,
        languageVersionSettings: LanguageVersionSettings,
        useJavac: Boolean
): ComponentProvider = createContainerForLazyResolveWithJava(
        moduleContext, bindingTrace, declarationProviderFactory, moduleContentScope, moduleClassResolver,
        CompilerEnvironment, lookupTracker, packagePartProvider, jvmTarget, languageVersionSettings,
        useBuiltInsProvider = true, useLazyResolve = false, useJavac = useJavac
)


fun ComponentProvider.initJvmBuiltInsForTopDownAnalysis() {
    get<JvmBuiltIns>().initialize(get<ModuleDescriptor>(), get<LanguageVersionSettings>())
}

internal fun JvmBuiltIns.initialize(module: ModuleDescriptor, languageVersionSettings: LanguageVersionSettings) {
    initialize(module, languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
}
