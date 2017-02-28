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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.backend.konan.KonanBuiltIns
import org.jetbrains.kotlin.backend.konan.KonanPlatform
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.context.ContextForNewModule
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory

object TopDownAnalyzerFacadeForKonan {
    fun analyzeFiles(files: Collection<KtFile>, config: KonanConfig): AnalysisResult {
        val moduleName = if (config.compileAsStdlib) {
            STDLIB_MODULE_NAME
        } else {
            Name.special("<${config.moduleId}>")
        }

        val context = ContextForNewModule(ProjectContext(config.project), moduleName, KonanPlatform.builtIns, null)

        val module = context.module
        assert (module.isStdlib() == config.compileAsStdlib)

        if (!module.isStdlib()) {
            context.setDependencies(listOf(module) + config.moduleDescriptors + KonanPlatform.builtIns.builtInsModule)
        } else {
            KonanPlatform.builtIns.createBuiltInsModule(module)
            assert (config.moduleDescriptors.isEmpty())
            context.setDependencies(module)

            // TODO: stdlib should probably also depend on builtInsModule.
            // However this would lead to mutual dependency between stdlib and builtInsModule,
            // and the compiler can't handle it.
        }

        return analyzeFilesWithGivenTrace(files, BindingTraceContext(), context, config)
    }

    fun analyzeFilesWithGivenTrace(
            files: Collection<KtFile>,
            trace: BindingTrace,
            moduleContext: ModuleContext,
            config: KonanConfig
    ): AnalysisResult {

        // we print out each file we compile for now
        files.forEach{println(it)}

        val analyzerForKonan = createTopDownAnalyzerForKonan(
                moduleContext, trace,
                FileBasedDeclarationProviderFactory(moduleContext.storageManager, files),
                config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)
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
