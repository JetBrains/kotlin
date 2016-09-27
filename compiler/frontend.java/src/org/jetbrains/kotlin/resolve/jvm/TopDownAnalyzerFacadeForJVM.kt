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
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.MutableModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.PackagePartProvider
import org.jetbrains.kotlin.frontend.java.di.createContainerForTopDownAnalyzerForJvm
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.load.java.lazy.ModuleClassResolverImpl
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.kotlin.DeserializationComponentsForJava
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackageFragmentProvider
import org.jetbrains.kotlin.load.kotlin.incremental.IncrementalPackagePartProvider
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.JvmBuiltIns
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.LazyTopDownAnalyzerForTopLevel
import org.jetbrains.kotlin.resolve.TopDownAnalysisMode
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisCompletedHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.PackageFragmentProviderExtension
import org.jetbrains.kotlin.resolve.jvm.platform.JvmPlatform
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import java.util.*

object TopDownAnalyzerFacadeForJVM {
    @JvmStatic
    fun analyzeFilesWithJavaIntegration(
            project: Project,
            files: Collection<KtFile>,
            trace: BindingTrace,
            configuration: CompilerConfiguration,
            packagePartProvider: PackagePartProvider
    ): AnalysisResult {
        val moduleContext = TopDownAnalyzerFacadeForJVM.createContextWithSealedModule(project, configuration)
        val storageManager = moduleContext.storageManager
        val module = moduleContext.module

        val incrementalComponents = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
        val lookupTracker = incrementalComponents?.getLookupTracker() ?: LookupTracker.DO_NOTHING
        val targetIds = configuration.get(JVMConfigurationKeys.MODULES)?.map(::TargetId)

        val resolverByClass = object : (JavaClass) -> JavaDescriptorResolver {
            lateinit var resolver: JavaDescriptorResolver

            override fun invoke(javaClass: JavaClass): JavaDescriptorResolver {
                return resolver
            }
        }

        val container = createContainerForTopDownAnalyzerForJvm(
                moduleContext,
                trace,
                FileBasedDeclarationProviderFactory(storageManager, files),
                GlobalSearchScope.allScope(project),
                lookupTracker,
                IncrementalPackagePartProvider.create(packagePartProvider, files, targetIds, incrementalComponents, storageManager),
                configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT),
                ModuleClassResolverImpl(resolverByClass)
        )
        resolverByClass.resolver = container.get<JavaDescriptorResolver>()

        val additionalProviders = ArrayList<PackageFragmentProvider>()

        if (incrementalComponents != null) {
            targetIds?.mapTo(additionalProviders) { targetId ->
                IncrementalPackageFragmentProvider(
                        files, module, storageManager, container.get<DeserializationComponentsForJava>().components,
                        incrementalComponents.getIncrementalCache(targetId), targetId
                )
            }
        }

        additionalProviders.add(container.get<JavaDescriptorResolver>().packageFragmentProvider)

        PackageFragmentProviderExtension.getInstances(project).mapNotNullTo(additionalProviders) { extension ->
            extension.getPackageFragmentProvider(project, module, storageManager, trace, null)
        }

        container.get<LazyTopDownAnalyzerForTopLevel>().analyzeFiles(TopDownAnalysisMode.TopLevelDeclarations, files, additionalProviders)

        for (extension in AnalysisCompletedHandlerExtension.getInstances(project)) {
            val result = extension.analysisCompleted(project, module, trace, files)
            if (result != null) return result
        }

        return AnalysisResult.success(trace.bindingContext, module)
    }

    fun createContextWithSealedModule(project: Project, configuration: CompilerConfiguration): MutableModuleContext {
        val projectContext = ProjectContext(project)
        val builtIns = JvmBuiltIns(projectContext.storageManager)
        val context = ContextForNewModule(
                projectContext, Name.special("<${configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)}>"),
                JvmPlatform, builtIns
        )
        context.setDependencies(context.module, builtIns.builtInsModule)
        return context
    }
}
