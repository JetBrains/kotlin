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

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltInsPackageFragmentProvider
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configurePlatformIndependentIncrementalCompilationComponents
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.AbstractJavaClassFinder
import org.jetbrains.kotlin.load.java.InternalFlexibleTypeTransformer
import org.jetbrains.kotlin.load.java.JavaClassFinderImpl
import org.jetbrains.kotlin.load.java.JavaClassesTracker
import org.jetbrains.kotlin.load.java.components.*
import org.jetbrains.kotlin.load.java.lazy.JavaResolverSettings
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformCompilerServices
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun createContainerForLazyResolveWithJava(
    targetPlaform: TargetPlatform,
    moduleContext: ModuleContext,
    bindingTrace: BindingTrace,
    declarationProviderFactory: DeclarationProviderFactory,
    moduleContentScope: GlobalSearchScope,
    moduleClassResolver: ModuleClassResolver,
    targetEnvironment: TargetEnvironment,
    lookupTracker: LookupTracker,
    expectActualTracker: ExpectActualTracker,
    packagePartProvider: PackagePartProvider,
    languageVersionSettings: LanguageVersionSettings,
    useBuiltInsProvider: Boolean,
    configureJavaClassFinder: (StorageComponentContainer.() -> Unit)? = null,
    javaClassTracker: JavaClassesTracker? = null
): StorageComponentContainer = createContainer("LazyResolveWithJava", JvmPlatformCompilerServices) {
    configureModule(moduleContext, moduleContentScope, targetPlaform, JvmPlatformCompilerServices, bindingTrace, languageVersionSettings)

    configurePlatformIndependentIncrementalCompilationComponents(lookupTracker, expectActualTracker)

    useImpl<ResolveSession>()
    useImpl<LazyTopDownAnalyzer>()
    useImpl<AnnotationResolverImpl>()
    useImpl<CompilerDeserializationConfiguration>()
    useImpl<ContractDeserializerImpl>()

    useInstance(packagePartProvider)
    useInstance(moduleClassResolver)
    useInstance(declarationProviderFactory)

    configureJavaSpecificComponents(
        moduleContext, moduleContentScope, useBuiltInsProvider, languageVersionSettings, configureJavaClassFinder, javaClassTracker
    )

    targetEnvironment.configure(this)

}.apply {
    get<AbstractJavaClassFinder>().initialize(bindingTrace, get<KotlinCodeAnalyzer>())
}

fun StorageComponentContainer.configureJavaSpecificComponents(
    moduleContext: ModuleContext,
    moduleContentScope: GlobalSearchScope,
    useBuiltInsProvider: Boolean,
    languageVersionSettings: LanguageVersionSettings,
    configureJavaClassFinder: (StorageComponentContainer.() -> Unit)?,
    javaClassTracker: JavaClassesTracker?
) {
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()
    useInstance(VirtualFileFinderFactory.getInstance(moduleContext.project).create(moduleContentScope))
    useInstance(JavaPropertyInitializerEvaluatorImpl)
    useImpl<SignaturePropagatorImpl>()
    useImpl<TraceBasedErrorReporter>()
    useInstance(InternalFlexibleTypeTransformer)
    useInstance(JavaDeprecationSettings)

    // configureJavaClassFinder != null <=> javac integration is enabled
    if (configureJavaClassFinder != null) {
        configureJavaClassFinder()
    } else {
        useImpl<JavaClassFinderImpl>()
        useImpl<LazyResolveBasedCache>()
        useImpl<JavaSourceElementFactoryImpl>()
    }

    useInstance(languageVersionSettings.getFlag(JvmAnalysisFlags.jsr305))

    // Currently, false in IDE environment (ideally, should be true everywhere)
    if (useBuiltInsProvider) {
        useInstance((moduleContext.module.builtIns as JvmBuiltIns).settings)
        useImpl<JvmBuiltInsPackageFragmentProvider>()
    }

    useInstance(javaClassTracker ?: JavaClassesTracker.Default)
    useInstance(
        JavaResolverSettings.create(isReleaseCoroutines = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines))
    )

    useImpl<FilesByFacadeFqNameIndexer>()
}

fun ComponentProvider.initJvmBuiltInsForTopDownAnalysis() {
    get<JvmBuiltIns>().initialize(get<ModuleDescriptor>(), get<LanguageVersionSettings>())
}

fun JvmBuiltIns.initialize(module: ModuleDescriptor, languageVersionSettings: LanguageVersionSettings) {
    initialize(module, languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
}
