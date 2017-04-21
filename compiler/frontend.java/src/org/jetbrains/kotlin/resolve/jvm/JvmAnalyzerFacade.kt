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

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService

class JvmPlatformParameters(
        val moduleByJavaClass: (JavaClass) -> ModuleInfo?
) : PlatformAnalysisParameters


object JvmAnalyzerFacade : AnalyzerFacade<JvmPlatformParameters>() {
    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: JvmPlatformParameters,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            languageSettingsProvider: LanguageSettingsProvider,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, moduleContext.storageManager, syntheticFiles,
                if (moduleInfo.isLibrary) GlobalSearchScope.EMPTY_SCOPE else moduleContentScope,
                moduleInfo
        )

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val referencedClassModule = platformParameters.moduleByJavaClass(javaClass)
            // We don't have full control over idea resolve api so we allow for a situation which should not happen in Kotlin.
            // For example, type in a java library can reference a class declared in a source root (is valid but rare case)
            // Providing a fallback strategy in this case can hide future problems, so we should at least log to be able to diagnose those

            @Suppress("UNCHECKED_CAST")
            val resolverForReferencedModule = referencedClassModule?.let { resolverForProject.tryGetResolverForModule(it as M) }

            val resolverForModule = resolverForReferencedModule ?: run {
                LOG.warn("Java referenced $referencedClassModule from $moduleInfo\nReferenced class was: $javaClass\n")
                // in case referenced class lies outside of our resolver, resolve the class as if it is inside our module
                // this leads to java class being resolved several times
                resolverForProject.resolverForModule(moduleInfo)
            }
            resolverForModule.componentProvider.get<JavaDescriptorResolver>()
        }

        val jvmTarget = languageSettingsProvider.getTargetPlatform(moduleInfo) as? JvmTarget ?: JvmTarget.JVM_1_6
        val languageVersionSettings = languageSettingsProvider.getLanguageVersionSettings(moduleInfo, project)

        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()

        val container = createContainerForLazyResolveWithJava(
                moduleContext,
                trace,
                declarationProviderFactory,
                moduleContentScope,
                moduleClassResolver,
                targetEnvironment,
                LookupTracker.DO_NOTHING,
                packagePartProvider,
                jvmTarget,
                languageVersionSettings,
                useBuiltInsProvider = false // TODO: load built-ins from module dependencies in IDE
        )

        StorageComponentContainerContributor.getInstances(project).forEach { it.onContainerComposed(container, moduleInfo) }

        val resolveSession = container.get<ResolveSession>()
        val javaDescriptorResolver = container.get<JavaDescriptorResolver>()

        val providersForModule = arrayListOf(
                resolveSession.packageFragmentProvider,
                javaDescriptorResolver.packageFragmentProvider)

        providersForModule += PackageFragmentProviderExtension.getInstances(project)
                .mapNotNull { it.getPackageFragmentProvider(project, moduleDescriptor, moduleContext.storageManager, trace, moduleInfo) }

        return ResolverForModule(CompositePackageFragmentProvider(providersForModule), container)
    }

    override val targetPlatform: TargetPlatform
        get() = JvmPlatform

    private val LOG = Logger.getInstance(JvmAnalyzerFacade::class.java)
}
