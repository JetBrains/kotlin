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
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.util.*

object TopDownAnalyzerFacadeForJVM {
    @JvmStatic
    fun analyzeFilesWithJavaIntegrationNoIncremental(
            moduleContext: ModuleContext,
            files: Collection<KtFile>,
            trace: BindingTrace,
            topDownAnalysisMode: TopDownAnalysisMode,
            packagePartProvider: PackagePartProvider
    ): AnalysisResult {
        return analyzeFilesWithJavaIntegration(moduleContext, files, trace, topDownAnalysisMode, null, null, packagePartProvider)
    }

    @JvmStatic
    fun analyzeFilesWithJavaIntegrationWithCustomContext(
            moduleContext: ModuleContext,
            files: Collection<KtFile>,
            trace: BindingTrace,
            modules: List<Module>?,
            incrementalCompilationComponents: IncrementalCompilationComponents?,
            packagePartProvider: PackagePartProvider
    ): AnalysisResult {
        return analyzeFilesWithJavaIntegration(
                moduleContext, files, trace, TopDownAnalysisMode.TopLevelDeclarations, modules, incrementalCompilationComponents,
                packagePartProvider
        )
    }

    private fun analyzeFilesWithJavaIntegration(
            moduleContext: ModuleContext,
            files: Collection<KtFile>,
            trace: BindingTrace,
            topDownAnalysisMode: TopDownAnalysisMode,
            modules: List<Module>?,
            incrementalComponents: IncrementalCompilationComponents?,
            packagePartProvider: PackagePartProvider
    ): AnalysisResult {
        val storageManager = moduleContext.storageManager
        val project = moduleContext.project
        val module = moduleContext.module

        val lookupTracker = incrementalComponents?.getLookupTracker() ?: LookupTracker.DO_NOTHING

        val targetIds = modules?.map(::TargetId)

        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                FileBasedDeclarationProviderFactory(storageManager, files),
                GlobalSearchScope.allScope(project),
                lookupTracker,
                IncrementalPackagePartProvider.create(packagePartProvider, targetIds, incrementalComponents, storageManager),
                LanguageVersionSettingsImpl.DEFAULT
        )

        val additionalProviders = ArrayList<PackageFragmentProvider>()

        if (incrementalComponents != null) {
            targetIds?.mapTo(additionalProviders) { targetId ->
                IncrementalPackageFragmentProvider(
                        files, module, storageManager, container.deserializationComponentsForJava.components,
                        incrementalComponents.getIncrementalCache(targetId), targetId
                )
            }
        }

        additionalProviders.add(container.javaDescriptorResolver.packageFragmentProvider)

        PackageFragmentProviderExtension.getInstances(project).mapNotNullTo(additionalProviders) { extension ->
            extension.getPackageFragmentProvider(project, module, storageManager, trace, null)
        }

        val analysisHandlerExtensions = AnalysisHandlerExtension.getInstances(project)

        fun invokeExtensionsOnAnalysisComplete(): AnalysisResult? {
            for (extension in analysisHandlerExtensions) {
                val result = extension.analysisCompleted(project, module, trace, files)
                if (result != null) return result
            }

            return null
        }

        for (extension in analysisHandlerExtensions) {
            val result = extension.doAnalysis(project, module, moduleContext, files, trace, container.container, additionalProviders)
            if (result != null) {
                invokeExtensionsOnAnalysisComplete()?.let { return it }
                return result
            }
        }

        container.lazyTopDownAnalyzerForTopLevel.analyzeFiles(topDownAnalysisMode, files, additionalProviders)

        invokeExtensionsOnAnalysisComplete()?.let { return it }

        return AnalysisResult.success(trace.bindingContext, module)
    }

    @JvmStatic
    fun createContextWithSealedModule(project: Project, moduleName: String): MutableModuleContext {
        val context = ContextForNewModule(
                project, Name.special("<$moduleName>"), JvmPlatform
        )
        context.setDependencies(context.module, JvmPlatform.builtIns.builtInsModule)
        return context
    }
}
