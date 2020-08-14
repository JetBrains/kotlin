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

package org.jetbrains.kotlin.resolve.jvm

import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService

class JvmPlatformParameters(
    val packagePartProviderFactory: (ModuleContent<*>) -> PackagePartProvider,
    val moduleByJavaClass: (JavaClass) -> ModuleInfo?,
    // params: referenced module info of target class, context module info of current resolver
    val resolverForReferencedModule: ((ModuleInfo, ModuleInfo) -> ResolverForModule?)? = null,
) : PlatformAnalysisParameters


class JvmResolverForModuleFactory(
    private val platformParameters: JvmPlatformParameters,
    private val targetEnvironment: TargetEnvironment,
    private val platform: TargetPlatform
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

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val referencedClassModule = platformParameters.moduleByJavaClass(javaClass)
            // A type in a java library can reference a class declared in a source root (is valid but rare case).
            // Resolving such a class with Kotlin resolver for libraries is guaranteed to fail, as libraries can't
            // have dependencies on the source roots. The chain of resolvers (sources -> libraries -> sdk) exists to prevent
            // potentially slow repetitive analysis of the same libraries after modifications in sources. The only way to mitigate
            // this restriction currently is to manually configure resolution anchors for known source-dependent libraries in a project.
            // See also KT-24309

            @Suppress("UNCHECKED_CAST")
            val resolverForReferencedModule = referencedClassModule?.let { referencedModuleInfo ->
                if (platformParameters.resolverForReferencedModule != null) {
                    platformParameters.resolverForReferencedModule.invoke(referencedModuleInfo, moduleInfo)
                } else {
                    resolverForProject.tryGetResolverForModule(referencedModuleInfo as M)
                }
            }

            val resolverForModule = resolverForReferencedModule?.takeIf {
                referencedClassModule.platform.isJvm()
            } ?: run {
                // in case referenced class lies outside of our resolver, resolve the class as if it is inside our module
                // this leads to java class being resolved several times
                resolverForProject.resolverForModule(moduleInfo)
            }
            resolverForModule.componentProvider.get<JavaDescriptorResolver>()
        }

        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()

        val lookupTracker = LookupTracker.DO_NOTHING
        val packagePartProvider = platformParameters.packagePartProviderFactory(moduleContent)
        val container = createContainerForLazyResolveWithJava(
            moduleDescriptor.platform!!,
            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
            moduleClassResolver,
            targetEnvironment,
            lookupTracker,
            ExpectActualTracker.DoNothing,
            packagePartProvider,
            languageVersionSettings,
            useBuiltInsProvider = false // TODO: load built-ins from module dependencies in IDE
        )

        val resolveSession = container.get<ResolveSession>()
        val javaDescriptorResolver = container.get<JavaDescriptorResolver>()

        val providersForModule = arrayListOf(
            resolveSession.packageFragmentProvider,
            javaDescriptorResolver.packageFragmentProvider
        )

        providersForModule +=
            PackageFragmentProviderExtension.getInstances(project)
                .mapNotNull {
                    it.getPackageFragmentProvider(
                        project, moduleDescriptor, moduleContext.storageManager, trace, moduleInfo, lookupTracker
                    )
                }

        return ResolverForModule(CompositePackageFragmentProvider(providersForModule), container)
    }
}
