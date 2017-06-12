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

package org.jetbrains.kotlin.checkers

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.hasError
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace

abstract class AbstractDiagnosticsTestWithJsStdLibAndBackendCompilation : AbstractDiagnosticsTestWithJsStdLib() {
    override fun analyzeModuleContents(
            moduleContext: ModuleContext,
            files: List<KtFile>,
            moduleTrace: BindingTrace,
            languageVersionSettings: LanguageVersionSettings,
            separateModules: Boolean
    ): JsAnalysisResult {
        val analysisResult = super.analyzeModuleContents(moduleContext, files, moduleTrace, languageVersionSettings, separateModules)
        val diagnostics = analysisResult.bindingTrace.bindingContext.diagnostics

        if (!hasError(diagnostics)) {
            val translator = K2JSTranslator(config)
            translator.translate(object : JsConfig.Reporter() {}, files, MainCallParameters.noCall(), analysisResult)
        }

        return analysisResult
    }
}
