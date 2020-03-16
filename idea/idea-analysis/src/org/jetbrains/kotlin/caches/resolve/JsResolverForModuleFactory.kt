/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.idea.klib.getCompatibilityInfo
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.js.DynamicTypeDeserializer
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.createKotlinJavascriptPackageFragmentProvider
import org.jetbrains.kotlin.serialization.konan.impl.KlibMetadataModuleDescriptorFactoryImpl
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils

class JsResolverForModuleFactory(
    private val targetEnvironment: TargetEnvironment
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
            project,
            moduleContext.storageManager,
            syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val container = createContainerForLazyResolve(
            moduleContext,
            declarationProviderFactory,
            BindingTraceContext(/* allowSliceRewrite = */ true),
            moduleDescriptor.platform!!,
            JsPlatformAnalyzerServices,
            targetEnvironment,
            languageVersionSettings
        )
        var packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

        val libraryProviders = createPackageFragmentProvider(moduleInfo, container, moduleContext, moduleDescriptor)

        if (libraryProviders.isNotEmpty()) {
            packageFragmentProvider = CompositePackageFragmentProvider(listOf(packageFragmentProvider) + libraryProviders)
        }

        return ResolverForModule(packageFragmentProvider, container)
    }
}

internal fun <M : ModuleInfo> createPackageFragmentProvider(
    moduleInfo: M,
    container: StorageComponentContainer,
    moduleContext: ModuleContext,
    moduleDescriptor: ModuleDescriptorImpl
): List<PackageFragmentProvider> = when (moduleInfo) {
    is JsKlibLibraryInfo -> {
        val library = moduleInfo.kotlinLibrary

        if (library.getCompatibilityInfo().isCompatible) {
            val metadataFactories = KlibMetadataFactories({ DefaultBuiltIns.Instance }, DynamicTypeDeserializer)

            val klibMetadataModuleDescriptorFactory = KlibMetadataModuleDescriptorFactoryImpl(
                metadataFactories.DefaultDescriptorFactory,
                metadataFactories.DefaultPackageFragmentsFactory,
                metadataFactories.flexibleTypeDeserializer
            )

            val packageFragmentNames = parseModuleHeader(library.moduleHeaderData).packageFragmentNameList

            val klibBasedPackageFragmentProvider = klibMetadataModuleDescriptorFactory.createPackageFragmentProvider(
                library,
                packageAccessHandler = null,
                packageFragmentNames = packageFragmentNames,
                storageManager = moduleContext.storageManager,
                moduleDescriptor = moduleDescriptor,
                configuration = CompilerDeserializationConfiguration(container.get()),
                compositePackageFragmentAddend = null
            )

            listOf(klibBasedPackageFragmentProvider)
        } else {
            emptyList()
        }
    }
    is LibraryModuleInfo -> {
        moduleInfo.getLibraryRoots()
            .flatMap { KotlinJavascriptMetadataUtils.loadMetadata(it) }
            .filter { it.version.isCompatible() }
            .map { metadata ->
                val (header, packageFragmentProtos) =
                    KotlinJavascriptSerializationUtil.readModuleAsProto(metadata.body, metadata.version)
                createKotlinJavascriptPackageFragmentProvider(
                    moduleContext.storageManager, moduleDescriptor, header, packageFragmentProtos, metadata.version,
                    container.get(), LookupTracker.DO_NOTHING
                )
            }
    }
    else -> emptyList()
}