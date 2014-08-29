/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.java

import org.jetbrains.jet.analyzer.AnalyzerFacade
import org.jetbrains.jet.analyzer.ResolverForModule
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.analyzer.PlatformAnalysisParameters
import org.jetbrains.jet.analyzer.ResolverForProject
import org.jetbrains.jet.lang.resolve.java.mapping.JavaToKotlinClassMap
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.jet.lang.resolve.BindingTraceContext
import com.intellij.openapi.project.Project
import org.jetbrains.jet.context.GlobalContext
import org.jetbrains.jet.lang.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass
import org.jetbrains.jet.lang.resolve.java.lazy.ModuleClassResolverImpl
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.jet.analyzer.ModuleInfo
import org.jetbrains.jet.analyzer.ModuleContent
import org.jetbrains.jet.di.InjectorForLazyResolveWithJava
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.openapi.components.ServiceManager
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import java.util.ArrayList
import org.jetbrains.jet.lang.resolve.android.searchAndAddAndroidDeclarations

public class JvmResolverForModule(
        override val lazyResolveSession: ResolveSession,
        public val javaDescriptorResolver: JavaDescriptorResolver
) : ResolverForModule

public class JvmPlatformParameters(
        public val moduleByJavaClass: (JavaClass) -> ModuleInfo
) : PlatformAnalysisParameters


public object JvmAnalyzerFacade : AnalyzerFacade<JvmResolverForModule, JvmPlatformParameters> {
    override fun <M : ModuleInfo> createResolverForModule(
            project: Project,
            globalContext: GlobalContext,
            moduleDescriptor: ModuleDescriptorImpl,
            moduleContent: ModuleContent,
            platformParameters: JvmPlatformParameters,
            resolverForProject: ResolverForProject<M, JvmResolverForModule>
    ): JvmResolverForModule {
        val (syntheticFiles, moduleContentScope) = moduleContent
        val filesToAnalyze = searchAndAddAndroidDeclarations(project, syntheticFiles)
        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
                project, globalContext.storageManager, filesToAnalyze, moduleContentScope
        )

        val moduleClassResolver = ModuleClassResolverImpl { javaClass ->
            val moduleInfo = platformParameters.moduleByJavaClass(javaClass)
            resolverForProject.resolverForModule(moduleInfo as M).javaDescriptorResolver
        }
        val injector = InjectorForLazyResolveWithJava(
                project, globalContext, moduleDescriptor, moduleContentScope, BindingTraceContext(), declarationProviderFactory, moduleClassResolver
        )

        val resolveSession = injector.getResolveSession()!!
        val javaDescriptorResolver = injector.getJavaDescriptorResolver()!!
        val providersForModule = listOf(resolveSession.getPackageFragmentProvider(), javaDescriptorResolver.packageFragmentProvider)
        moduleDescriptor.initialize(CompositePackageFragmentProvider(providersForModule))
        return JvmResolverForModule(resolveSession, javaDescriptorResolver)
    }

    override val defaultImports = TopDownAnalyzerFacadeForJVM.DEFAULT_IMPORTS
    override val platformToKotlinClassMap = JavaToKotlinClassMap.getInstance()

}