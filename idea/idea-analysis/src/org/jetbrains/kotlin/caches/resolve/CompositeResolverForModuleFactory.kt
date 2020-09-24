/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("Duplicates")

package org.jetbrains.kotlin.caches.resolve

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.analyzer.common.CommonAnalysisParameters
import org.jetbrains.kotlin.analyzer.common.configureCommonSpecificComponents
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.frontend.di.configureStandardResolveComponents
import org.jetbrains.kotlin.frontend.java.di.configureJavaSpecificComponents
import org.jetbrains.kotlin.frontend.java.di.initializeJavaSpecificComponents
import org.jetbrains.kotlin.idea.project.IdeaEnvironment
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolver
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.*
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.JvmPlatform
import org.jetbrains.kotlin.platform.konan.NativePlatform
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.inference.InferenceCompatibilityCheckerImpl
import org.jetbrains.kotlin.resolve.checkers.ExperimentalMarkerDeclarationAnnotationChecker
import org.jetbrains.kotlin.resolve.jvm.JavaDescriptorResolver
import org.jetbrains.kotlin.resolve.jvm.JvmPlatformParameters
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.deserialization.MetadataPackageFragmentProvider
import org.jetbrains.kotlin.serialization.deserialization.MetadataPartProvider
import org.jetbrains.kotlin.storage.StorageManager

class CompositeResolverForModuleFactory(
    private val commonAnalysisParameters: CommonAnalysisParameters,
    private val jvmAnalysisParameters: JvmPlatformParameters,
    private val targetPlatform: TargetPlatform,
    private val compilerServices: CompositeAnalyzerServices
) : ResolverForModuleFactory() {
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings
    ): ResolverForModule {
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val metadataPartProvider = commonAnalysisParameters.metadataPartProviderFactory(moduleContent)
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()


        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val referencedClassModule = jvmAnalysisParameters.moduleByJavaClass(javaClass)
            // We don't have full control over idea resolve api so we allow for a situation which should not happen in Kotlin.
            // For example, type in a java library can reference a class declared in a source root (is valid but rare case)
            // Providing a fallback strategy in this case can hide future problems, so we should at least log to be able to diagnose those

            @Suppress("UNCHECKED_CAST")
            val resolverForReferencedModule = referencedClassModule?.let { resolverForProject.tryGetResolverForModule(it as M) }

            val resolverForModule = resolverForReferencedModule?.takeIf {
                referencedClassModule.platform.has<JvmPlatform>()
            } ?: run {
                // in case referenced class lies outside of our resolver, resolve the class as if it is inside our module
                // this leads to java class being resolved several times
                resolverForProject.resolverForModule(moduleInfo)
            }
            resolverForModule.componentProvider.get<JavaDescriptorResolver>()
        }

        val packagePartProvider = jvmAnalysisParameters.packagePartProviderFactory(moduleContent)

        val container = createContainerForCompositePlatform(
            moduleContext, moduleContentScope, languageVersionSettings, targetPlatform,
            compilerServices, trace, declarationProviderFactory, metadataPartProvider,
            moduleClassResolver, packagePartProvider
        )

        val packageFragmentProviders = sequence {
            yield(container.get<ResolveSession>().packageFragmentProvider)

            yieldAll(getCommonProvidersIfAny(moduleInfo, moduleContext, moduleDescriptor, container)) // todo: module context
            yieldAll(getJsProvidersIfAny(moduleInfo, moduleContext, moduleDescriptor, container))
            yieldAll(getJvmProvidersIfAny(container))
            yieldAll(getNativeProvidersIfAny(moduleInfo, container))
        }.toList()

        return ResolverForModule(CompositePackageFragmentProvider(packageFragmentProviders), container)
    }

    private fun getCommonProvidersIfAny(
        moduleInfo: ModuleInfo,
        moduleContext: ModuleContext,
        moduleDescriptor: ModuleDescriptor,
        container: StorageComponentContainer
    ): List<PackageFragmentProvider> {
        if (!targetPlatform.isCommon()) return emptyList()

        val metadataProvider = container.get<MetadataPackageFragmentProvider>()

        val klibMetadataProvider = CommonPlatforms.defaultCommonPlatform.idePlatformKind.resolution.createKlibPackageFragmentProvider(
            moduleInfo,
            moduleContext.storageManager,
            container.get<LanguageVersionSettings>(),
            moduleDescriptor
        )

        return listOfNotNull(metadataProvider, klibMetadataProvider)
    }

    private fun getJvmProvidersIfAny(container: StorageComponentContainer): List<PackageFragmentProvider> =
        if (targetPlatform.has<JvmPlatform>()) listOf(container.get<JavaDescriptorResolver>().packageFragmentProvider) else emptyList()

    private fun getNativeProvidersIfAny(moduleInfo: ModuleInfo, container: StorageComponentContainer): List<PackageFragmentProvider> {
        if (!targetPlatform.has<NativePlatform>()) return emptyList()

        return listOfNotNull(
            NativePlatforms.unspecifiedNativePlatform.idePlatformKind.resolution.createKlibPackageFragmentProvider(
                moduleInfo,
                container.get<StorageManager>(),
                container.get<LanguageVersionSettings>(),
                container.get<ModuleDescriptor>()
            )
        )
    }

    private fun getJsProvidersIfAny(
        moduleInfo: ModuleInfo,
        moduleContext: ModuleContext,
        moduleDescriptor: ModuleDescriptorImpl,
        container: StorageComponentContainer
    ): List<PackageFragmentProvider> {
        if (moduleInfo !is LibraryModuleInfo || !moduleInfo.platform.isJs()) return emptyList()

        return createPackageFragmentProvider(moduleInfo, container, moduleContext, moduleDescriptor)
    }

    fun createContainerForCompositePlatform(
        moduleContext: ModuleContext,
        moduleContentScope: GlobalSearchScope,
        languageVersionSettings: LanguageVersionSettings,
        targetPlatform: TargetPlatform,
        analyzerServices: CompositeAnalyzerServices,
        trace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        metadataPartProvider: MetadataPartProvider,
        // Guaranteed to be non-null for modules with JVM
        moduleClassResolver: ModuleClassResolver?,
        packagePartProvider: PackagePartProvider?
    ): StorageComponentContainer = composeContainer("CompositePlatform") {
        // Shared by all PlatformConfigurators
        configureDefaultCheckers()

        // Specific for each PlatformConfigurator
        for (configurator in analyzerServices.services.map { it.platformConfigurator as PlatformConfiguratorBase }) {
            configurator.configureExtensionsAndCheckers(this)
        }

        // Called by all normal containers set-ups
        configureModule(moduleContext, targetPlatform, analyzerServices, trace, languageVersionSettings)
        configureStandardResolveComponents()
        useInstance(moduleContentScope)
        useInstance(declarationProviderFactory)

        // Probably, should be in StandardResolveComponents, but
        useInstance(VirtualFileFinderFactory.getInstance(moduleContext.project).create(moduleContentScope))
        useInstance(packagePartProvider!!)

        // JVM-specific
        if (targetPlatform.has<JvmPlatform>()) {
            configureJavaSpecificComponents(
                moduleContext, moduleClassResolver!!, languageVersionSettings, configureJavaClassFinder = null,
                javaClassTracker = null,
                useBuiltInsProvider = false
            )
        }

        // Common-specific
        if (targetPlatform.isCommon()) {
            configureCommonSpecificComponents()
        }

        IdeaEnvironment.configure(this)
    }.apply {
        if (targetPlatform.has<JvmPlatform>()) {
            initializeJavaSpecificComponents(trace)
        }
    }
}

class CompositeAnalyzerServices(val services: List<PlatformDependentAnalyzerServices>) : PlatformDependentAnalyzerServices() {
    override val platformConfigurator: PlatformConfigurator = CompositePlatformConigurator(services.map { it.platformConfigurator })

    override fun computePlatformSpecificDefaultImports(storageManager: StorageManager, result: MutableList<ImportPath>) {
        val intersectionOfDefaultImports = services.map { service ->
            mutableListOf<ImportPath>()
                .apply { service.computePlatformSpecificDefaultImports(storageManager, this) }
                .toSet()
        }.safeIntersect()

        result.addAll(intersectionOfDefaultImports)
    }

    override val defaultLowPriorityImports: List<ImportPath> = services.map { it.defaultLowPriorityImports.toSet() }.safeIntersect()

    override val excludedImports: List<FqName> = services.map { it.excludedImports.toSet() }.safeUnion()

    private fun <T> List<Set<T>>.safeUnion(): List<T> =
        if (isEmpty()) emptyList() else reduce { first, second -> first.union(second) }.toList()

    private fun <T> List<Set<T>>.safeIntersect(): List<T> =
        if (isEmpty()) emptyList() else reduce { first, second -> first.intersect(second) }.toList()
}

class CompositePlatformConigurator(private val componentConfigurators: List<PlatformConfigurator>) : PlatformConfigurator {
    override val platformSpecificContainer: StorageComponentContainer
        get() = composeContainer(this::class.java.simpleName) {
            configureDefaultCheckers()
            for (configurator in componentConfigurators) {
                (configurator as PlatformConfiguratorBase).configureExtensionsAndCheckers(this)
            }
        }

    override fun configureModuleComponents(container: StorageComponentContainer) {
        componentConfigurators.forEach { it.configureModuleComponents(container) }
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        // We (ab)use the fact that currently, platforms don't use that method, so the only injected compnent will be
        // ExperimentalMarkerDeclarationAnnotationChecker.
        // Unfortunately, it is declared in base class, so repeating call to 'configureModuleDependentCheckers' will lead
        // to multiple registrrations.
        container.useImpl<ExperimentalMarkerDeclarationAnnotationChecker>()
        container.useImpl<InferenceCompatibilityCheckerImpl>()
    }
}