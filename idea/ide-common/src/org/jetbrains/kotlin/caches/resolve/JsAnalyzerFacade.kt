/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.caches.resolve.LibraryModuleInfo
import org.jetbrains.kotlin.config.TargetPlatformVersion
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.createContainerForLazyResolve
import org.jetbrains.kotlin.js.resolve.JsPlatform
import org.jetbrains.kotlin.resolve.BindingTraceContext
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.deserialization.DeserializationConfiguration
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import org.jetbrains.kotlin.utils.KotlinJavascriptMetadataUtils

object JsAnalyzerFacade : AnalyzerFacade() {

    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: PlatformAnalysisParameters,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            languageSettingsProvider: LanguageSettingsProvider,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project,
                moduleContext.storageManager,
                syntheticFiles,
                if (moduleInfo.isLibrary) GlobalSearchScope.EMPTY_SCOPE else moduleContentScope,
                moduleInfo
        )

        val container = createContainerForLazyResolve(
                moduleContext,
                declarationProviderFactory,
                BindingTraceContext(),
                JsPlatform,
                TargetPlatformVersion.NoVersion,
                targetEnvironment,
                languageSettingsProvider.getLanguageVersionSettings(moduleInfo, project)
        )
        var packageFragmentProvider = container.get<ResolveSession>().packageFragmentProvider

        if (moduleInfo is LibraryModuleInfo && moduleInfo.platform == JsPlatform) {
            val providers = moduleInfo.getLibraryRoots()
                    .flatMap { KotlinJavascriptMetadataUtils.loadMetadata(it) }
                    .filter { it.version.isCompatible() }
                    .mapNotNull {
                        KotlinJavascriptSerializationUtil.readModule(
                                it.body, moduleContext.storageManager, moduleDescriptor, container.get<DeserializationConfiguration>()
                        ).data
                    }

            if (providers.isNotEmpty()) {
                packageFragmentProvider = CompositePackageFragmentProvider(listOf(packageFragmentProvider) + providers)
            }
        }

        return ResolverForModule(packageFragmentProvider, container)
    }

    override val targetPlatform: TargetPlatform
        get() = JsPlatform
}
