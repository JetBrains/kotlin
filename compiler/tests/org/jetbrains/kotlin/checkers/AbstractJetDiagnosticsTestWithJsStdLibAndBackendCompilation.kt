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

import org.jetbrains.kotlin.context.GlobalContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS
import org.jetbrains.kotlin.js.analyzer.JsAnalysisResult
import org.jetbrains.kotlin.js.facade.K2JSTranslator
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.facade.exceptions.TranslationException
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics

import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.hasError

public abstract class AbstractJetDiagnosticsTestWithJsStdLibAndBackendCompilation : AbstractJetDiagnosticsTestWithJsStdLib() {
    override fun analyzeModuleContents(
            context: GlobalContext,
            jetFiles: List<JetFile>,
            module: ModuleDescriptorImpl,
            moduleTrace: BindingTrace
    ) {
        val analysisResult = TopDownAnalyzerFacadeForJS.analyzeFilesWithGivenTrace(jetFiles, moduleTrace, module, getConfig())
        val diagnostics = analysisResult.bindingTrace.getBindingContext().getDiagnostics()

        if (!hasError(diagnostics)) {
            val translator = K2JSTranslator(getConfig())
            translator.translate(jetFiles, MainCallParameters.noCall(), analysisResult)
        }
    }
}
