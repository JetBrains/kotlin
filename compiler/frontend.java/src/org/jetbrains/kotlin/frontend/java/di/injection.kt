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
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.frontend.di.configureIncrementalCompilation
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
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
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.tower.ImplicitsExtensionsResolutionFilter
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JvmDiagnosticComponents
import org.jetbrains.kotlin.resolve.jvm.multiplatform.OptionalAnnotationPackageFragmentProvider
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory

fun createContainerForLazyResolveWithJava(
    jvmPlatform: TargetPlatform,
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
    javaClassTracker: JavaClassesTracker? = null,
    implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter? = null,
): StorageComponentContainer = createContainer("LazyResolveWithJava", JvmPlatformAnalyzerServices) {
    configureModule(moduleContext, jvmPlatform, JvmPlatformAnalyzerServices, bindingTrace, languageVersionSettings)

    configureIncrementalCompilation(lookupTracker, expectActualTracker)
    configureStandardResolveComponents()

    useInstance(moduleContentScope)
    useInstance(packagePartProvider)
    useInstance(declarationProviderFactory)
    useInstanceIfNotNull(implicitsResolutionFilter)

    useInstance(VirtualFileFinderFactory.getInstance(moduleContext.project).create(moduleContentScope))

    configureJavaSpecificComponents(
        moduleContext, moduleClassResolver, languageVersionSettings, configureJavaClassFinder,
        javaClassTracker, useBuiltInsProvider
    )

    targetEnvironment.configure(this)

}.apply {
    initializeJavaSpecificComponents(bindingTrace)
}

fun StorageComponentContainer.initializeJavaSpecificComponents(bindingTrace: BindingTrace) {
    get<AbstractJavaClassFinder>().initialize(bindingTrace, get<KotlinCodeAnalyzer>())
}

fun StorageComponentContainer.configureJavaSpecificComponents(
    moduleContext: ModuleContext,
    moduleClassResolver: ModuleClassResolver,
    languageVersionSettings: LanguageVersionSettings,
    configureJavaClassFinder: (StorageComponentContainer.() -> Unit)?,
    javaClassTracker: JavaClassesTracker?,
    useBuiltInsProvider: Boolean
) {
    useImpl<JavaDescriptorResolver>()
    useImpl<DeserializationComponentsForJava>()
    useInstance(JavaPropertyInitializerEvaluatorImpl)
    useImpl<SignaturePropagatorImpl>()
    useImpl<TraceBasedErrorReporter>()
    useInstance(InternalFlexibleTypeTransformer)
    useInstance(JavaDeprecationSettings)
    useInstance(moduleClassResolver)

    if (configureJavaClassFinder != null) {
        configureJavaClassFinder()
    } else {
        useImpl<JavaClassFinderImpl>()
        useImpl<LazyResolveBasedCache>()
        useImpl<JavaSourceElementFactoryImpl>()
    }

    useInstance(languageVersionSettings.getFlag(JvmAnalysisFlags.jsr305))

    if (useBuiltInsProvider) {
        useInstance((moduleContext.module.builtIns as JvmBuiltIns).settings)
        useImpl<JvmBuiltInsPackageFragmentProvider>()
    }
    useImpl<OptionalAnnotationPackageFragmentProvider>()

    useInstance(javaClassTracker ?: JavaClassesTracker.Default)
    useInstance(
        JavaResolverSettings.create(isReleaseCoroutines = languageVersionSettings.supportsFeature(LanguageFeature.ReleaseCoroutines))
    )

    useImpl<FilesByFacadeFqNameIndexer>()
    useImpl<JvmDiagnosticComponents>()
}

fun ComponentProvider.initJvmBuiltInsForTopDownAnalysis() {
    get<JvmBuiltIns>().initialize(get<ModuleDescriptor>(), get<LanguageVersionSettings>())
}

fun JvmBuiltIns.initialize(module: ModuleDescriptor, languageVersionSettings: LanguageVersionSettings) {
    initialize(module, languageVersionSettings.supportsFeature(LanguageFeature.AdditionalBuiltInsMembers))
}
