/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.analyzer

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.TargetPlatformVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.ide.konan.NativeLibraryInfo
import org.jetbrains.kotlin.ide.konan.createPackageFragmentProvider
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.DefaultBuiltInPlatforms
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformCompilerServices
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService.Companion.createDeclarationProviderFactory

object NativeResolverForModuleFactory : ResolverForModuleFactory() {
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings
    ): ResolverForModule {

        val declarationProviderFactory = createDeclarationProviderFactory(
            moduleContext.project,
            moduleContext.storageManager,
            moduleContent.syntheticFiles,
            moduleContent.moduleContentScope,
            moduleContent.moduleInfo
        )

        val container = createContainerForLazyResolve(
            moduleContext,
            declarationProviderFactory,
            CodeAnalyzerInitializer.getInstance(moduleContext.project).createTrace(),
            DefaultBuiltInPlatforms.konanPlatform,
            NativePlatformCompilerServices,
            targetEnvironment,
            languageVersionSettings
        )

        val packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider
        val fragmentProviders = mutableListOf(packageFragmentProvider)

        val moduleInfo = moduleContent.moduleInfo

        val konanLibrary = moduleInfo.getCapability(NativeLibraryInfo.NATIVE_LIBRARY_CAPABILITY)
        if (konanLibrary != null) {
            val libPackageFragmentProvider =
                konanLibrary.createPackageFragmentProvider(
                    moduleContext.storageManager,
                    languageVersionSettings,
                    moduleDescriptor
                )

            fragmentProviders.add(libPackageFragmentProvider)
        }

        return ResolverForModule(CompositePackageFragmentProvider(fragmentProviders), container)
    }
}
