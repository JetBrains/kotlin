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
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.context.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.java.di.ContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.frontend.java.di.*
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCompilationComponents
import org.jetbrains.kotlin.modules.Module
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.modules.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

import java.util.ArrayList

object TopDownAnalyzerFacadeForJVM {
    @JvmStatic
    fun analyzeFilesWithJavaIntegration(
            moduleContext: ModuleContext,
            files: Collection<KtFile>,
            trace: BindingTrace,
            configuration: CompilerConfiguration,
            packagePartProvider: PackagePartProvider
    ): AnalysisResult {
        var packagePartProvider = packagePartProvider
        val project = moduleContext.project

        val providerFactory = FileBasedDeclarationProviderFactory(moduleContext.storageManager, files)

        val incrementalCompilationComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        val lookupTracker = if (incrementalCompilationComponents != null)
            incrementalCompilationComponents.getLookupTracker()
        else
            LookupTracker.DO_NOTHING

        var targetIds: MutableList<TargetId>? = null
        val modules = configuration.get(JVMConfigurationKeys.MODULES)
        if (modules != null) {
            targetIds = ArrayList<TargetId>(modules.size)

            for (module in modules) {
                targetIds.add(TargetId(module))
            }
        }

        packagePartProvider = IncrementalPackagePartProvider.create(
                packagePartProvider, files, targetIds, incrementalCompilationComponents, moduleContext.storageManager
        )

        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                providerFactory,
                GlobalSearchScope.allScope(project),
                lookupTracker,
                packagePartProvider,
                configuration.get<LanguageVersionSettings>(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
        )

        val additionalProviders = ArrayList<PackageFragmentProvider>()

        if (targetIds != null && incrementalCompilationComponents != null) {
            for (targetId in targetIds) {
                val incrementalCache = incrementalCompilationComponents.getIncrementalCache(targetId)

                additionalProviders.add(
                        IncrementalPackageFragmentProvider(
                                files, moduleContext.module, moduleContext.storageManager,
                                container.deserializationComponentsForJava.components,
                                incrementalCache, targetId
                        )
                )
            }
        }
        additionalProviders.add(container.javaDescriptorResolver.packageFragmentProvider)

        for (extension in PackageFragmentProviderExtension.getInstances(project)) {
            val provider = extension.getPackageFragmentProvider(
                    project, moduleContext.module, moduleContext.storageManager, trace, null)
            if (provider != null) additionalProviders.add(provider)
        }

        container.lazyTopDownAnalyzerForTopLevel.analyzeFiles(TopDownAnalysisMode.TopLevelDeclarations, files, additionalProviders)

        val bindingContext = trace.bindingContext
        val module = moduleContext.module

        val analysisCompletedHandlerExtensions = AnalysisCompletedHandlerExtension.getInstances(moduleContext.project)

        for (extension in analysisCompletedHandlerExtensions) {
            val result = extension.analysisCompleted(project, module, trace, files)
            if (result != null) return result
        }

        return AnalysisResult.success(bindingContext, module)
    }

    @JvmStatic
    fun createContextWithSealedModule(
            project: Project, configuration: CompilerConfiguration
    ): MutableModuleContext {
        val projectContext = ProjectContext(project)
        val builtIns = JvmBuiltIns(projectContext.storageManager)
        val context = ContextForNewModule(
                projectContext, Name.special("<" + configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME) + ">"),
                JvmPlatform, builtIns
        )
        context.setDependencies(context.module, builtIns.builtInsModule)
        return context
    }
}
