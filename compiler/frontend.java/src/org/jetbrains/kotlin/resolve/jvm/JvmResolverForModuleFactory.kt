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
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.TargetPlatformVersion
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
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService

class JvmPlatformParameters(
    val packagePartProviderFactory: (ModuleContent<*>) -> PackagePartProvider,
    val moduleByJavaClass: (JavaClass) -> ModuleInfo?
) : PlatformAnalysisParameters


object JvmResolverForModuleFactory : ResolverForModuleFactory() {
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        platformParameters: PlatformAnalysisParameters,
        targetEnvironment: TargetEnvironment,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        targetPlatformVersion: TargetPlatformVersion
    ): ResolverForModule {
        val (moduleInfo, syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            moduleInfo
        )

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val referencedClassModule = (platformParameters as JvmPlatformParameters).moduleByJavaClass(javaClass)
            // We don't have full control over idea resolve api so we allow for a situation which should not happen in Kotlin.
            // For example, type in a java library can reference a class declared in a source root (is valid but rare case)
            // Providing a fallback strategy in this case can hide future problems, so we should at least log to be able to diagnose those

            @Suppress("UNCHECKED_CAST")
            val resolverForReferencedModule = referencedClassModule?.let { resolverForProject.tryGetResolverForModule(it as M) }

            val resolverForModule = resolverForReferencedModule?.takeIf {
                referencedClassModule.platform == JvmPlatform || referencedClassModule.platform == null
            } ?: run {
                // in case referenced class lies outside of our resolver, resolve the class as if it is inside our module
                // this leads to java class being resolved several times
                resolverForProject.resolverForModule(moduleInfo)
            }
            resolverForModule.componentProvider.get<JavaDescriptorResolver>()
        }

        val jvmTarget = targetPlatformVersion as? JvmTarget ?: JvmTarget.DEFAULT
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()

        val lookupTracker = LookupTracker.DO_NOTHING
        val packagePartProvider = (platformParameters as JvmPlatformParameters).packagePartProviderFactory(moduleContent)
        val container = createContainerForLazyResolveWithJava(
            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
            moduleClassResolver,
            targetEnvironment,
            lookupTracker,
            ExpectActualTracker.DoNothing,
            packagePartProvider,
            jvmTarget,
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
