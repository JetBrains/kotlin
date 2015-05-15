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

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.di.InjectorForLazyResolveWithJava
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import java.util.ArrayList
import kotlin.platform.platformStatic

public class JvmResolverForModule(
        override val lazyResolveSession: ResolveSession,
        public val javaDescriptorResolver: JavaDescriptorResolver
) : ResolverForModule

public class JvmPlatformParameters(
        public val moduleByJavaClass: (JavaClass) -> ModuleInfo
) : PlatformAnalysisParameters


public object JvmAnalyzerFacade : AnalyzerFacade<JvmResolverForModule, JvmPlatformParameters> {
    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: JvmPlatformParameters,
            resolverForProject: ResolverForProject<M, JvmResolverForModule>
    ): JvmResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val filesToAnalyze = getAllFilesToAnalyze(project, moduleInfo, syntheticFiles)
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, moduleContext.storageManager, filesToAnalyze,
                if (moduleInfo.isLibrary) GlobalSearchScope.EMPTY_SCOPE else moduleContentScope
        )

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val moduleInfo = platformParameters.moduleByJavaClass(javaClass)
            resolverForProject.resolverForModule(moduleInfo as M).javaDescriptorResolver
        }
        val injector = InjectorForLazyResolveWithJava(
                moduleContext,
                CodeAnalyzerInitializer.getInstance(project).createTrace(),
                declarationProviderFactory,
                moduleContentScope,
                moduleClassResolver
        )

        val resolveSession = injector.getResolveSession()!!
        val javaDescriptorResolver = injector.getJavaDescriptorResolver()!!
        val providersForModule = listOf(resolveSession.getPackageFragmentProvider(), javaDescriptorResolver.packageFragmentProvider)
        moduleContext.module.initialize(CompositePackageFragmentProvider(providersForModule))
        return JvmResolverForModule(resolveSession, javaDescriptorResolver)
    }

    override val defaultImports = TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS
    override val platformToKotlinClassMap = JavaToKotlinClassMap.INSTANCE

    public platformStatic fun getAllFilesToAnalyze(project: Project, moduleInfo: ModuleInfo?, baseFiles: Collection<JetFile>): List<JetFile> {
        val allFiles = ArrayList(baseFiles)
        for (externalDeclarationsProvider in ExternalDeclarationsProvider.getInstances(project)) {
            allFiles.addAll(externalDeclarationsProvider.getExternalDeclarations(moduleInfo))
        }
        return allFiles
    }

}
