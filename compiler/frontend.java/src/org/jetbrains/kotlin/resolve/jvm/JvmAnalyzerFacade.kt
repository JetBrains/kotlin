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
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.ModuleParameters
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.extensions.ExternalDeclarationsProvider
import org.jetbrains.kotlin.frontend.java.di.createContainerForLazyResolveWithJava
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.CodeAnalyzerInitializer
import org.jetbrains.kotlin.resolve.TargetEnvironment
import org.jetbrains.kotlin.resolve.TargetPlatform
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import java.util.ArrayList

public class JvmPlatformParameters(
        public val moduleByJavaClass: (JavaClass) -> ModuleInfo
) : PlatformAnalysisParameters


public object JvmAnalyzerFacade : AnalyzerFacade<JvmPlatformParameters>() {
    override fun <M : ModuleInfo> createResolverForModule(
            moduleInfo: M,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContext: ModuleContext,
            moduleContent: ModuleContent,
            platformParameters: JvmPlatformParameters,
            targetEnvironment: TargetEnvironment,
            resolverForProject: ResolverForProject<M>,
            packagePartProvider: PackagePartProvider
    ): ResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val project = moduleContext.project
        val filesToAnalyze = getAllFilesToAnalyze(project, moduleInfo, syntheticFiles)
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, moduleContext.storageManager, filesToAnalyze,
                if (moduleInfo.isLibrary) GlobalSearchScope.EMPTY_SCOPE else moduleContentScope
        )

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val moduleInfo = platformParameters.moduleByJavaClass(javaClass)
            resolverForProject.resolverForModule(moduleInfo as M).componentProvider.get<JavaDescriptorResolver>()
        }
        val container = createContainerForLazyResolveWithJava(
                moduleContext,
                CodeAnalyzerInitializer.getInstance(project).createTrace(),
                declarationProviderFactory,
                moduleContentScope,
                moduleClassResolver,
                targetEnvironment,
                packagePartProvider
        )
        val resolveSession = container.get<ResolveSession>()
        val javaDescriptorResolver = container.get<JavaDescriptorResolver>()

        val providersForModule = listOf(resolveSession.getPackageFragmentProvider(), javaDescriptorResolver.packageFragmentProvider)
        return ResolverForModule(CompositePackageFragmentProvider(providersForModule), container)
    }

    @JvmStatic
    public fun getAllFilesToAnalyze(project: Project, moduleInfo: ModuleInfo?, baseFiles: Collection<KtFile>): List<KtFile> {
        val allFiles = ArrayList(baseFiles)
        for (externalDeclarationsProvider in ExternalDeclarationsProvider.getInstances(project)) {
            allFiles.addAll(externalDeclarationsProvider.getExternalDeclarations(moduleInfo))
        }
        return allFiles
    }

    override val targetPlatform: TargetPlatform
        get() = JvmPlatform
}
