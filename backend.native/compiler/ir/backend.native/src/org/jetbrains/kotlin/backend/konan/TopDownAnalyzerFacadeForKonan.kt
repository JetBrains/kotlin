/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.descriptors.createKonanModuleDescriptor
import org.jetbrains.kotlin.backend.konan.descriptors.CurrentKonanModule
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.MutableModuleContextImpl
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

object TopDownAnalyzerFacadeForKonan {
    fun analyzeFiles(files: Collection<KtFile>, config: KonanConfig): AnalysisResult {
        val moduleName = Name.special("<${config.moduleId}>") 

        val projectContext = ProjectContext(config.project)

        val module = createKonanModuleDescriptor(moduleName, projectContext.storageManager, origin = CurrentKonanModule)
        val context = MutableModuleContextImpl(module, projectContext)

        if (!module.isStdlib()) {
            val dependencies = listOf(module) + config.moduleDescriptors +
                    config.getOrCreateForwardDeclarationsModule(module.builtIns, projectContext.storageManager)
            module.setDependencies(dependencies, config.friends)
        } else {
            assert (config.moduleDescriptors.isEmpty())
            context.setDependencies(module)
        }

        return analyzeFilesWithGivenTrace(files, BindingTraceContext(), context, config)
    }

    fun analyzeFilesWithGivenTrace(
            files: Collection<KtFile>,
            trace: BindingTrace,
            moduleContext: ModuleContext,
            config: KonanConfig
    ): AnalysisResult {

        // we print out each file we compile if frontend phase is verbose
        files.takeIf { with (KonanPhases) {
            phases[known(KonanPhase.FRONTEND.visibleName)]!!.verbose
        }} ?.forEach(::println)

        val analyzerForKonan = createTopDownAnalyzerForKonan(
                moduleContext, trace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!
        )

        analyzerForKonan.analyzeDeclarations(TopDownAnalysisMode.TopLevelDeclarations, files)
        return AnalysisResult.success(trace.bindingContext, moduleContext.module)
    }

    fun checkForErrors(files: Collection<KtFile>, bindingContext: BindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext)
        for (file in files) {
            AnalyzingUtils.checkForSyntacticErrors(file)
        }
    }
}
