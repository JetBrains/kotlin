/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.caches.resolve

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.js.resolve.JsPlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.serialization.js.createKotlinJavascriptPackageFragmentProvider
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

        val libraryProviders = (moduleInfo as? LibraryModuleInfo)?.getLibraryRoots().orEmpty()
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

        if (libraryProviders.isNotEmpty()) {
            packageFragmentProvider = CompositePackageFragmentProvider(listOf(packageFragmentProvider) + libraryProviders)
        }

        return ResolverForModule(packageFragmentProvider, container)
    }
}
