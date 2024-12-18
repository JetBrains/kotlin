/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

interface CodegenFactory {
    class IrConversionInput(
        val project: Project,
        val files: Collection<KtFile>,
        val configuration: CompilerConfiguration,
        val module: ModuleDescriptor,
        val diagnosticReporter: DiagnosticReporter,
        val bindingContext: BindingContext,
        val languageVersionSettings: LanguageVersionSettings,
        val ignoreErrors: Boolean,
        val skipBodies: Boolean,
    ) {
        companion object {
            fun fromGenerationStateAndFiles(
                state: GenerationState, files: Collection<KtFile>, bindingContext: BindingContext,
            ): IrConversionInput =
                with(state) {
                    IrConversionInput(
                        project, files, configuration, module, state.diagnosticReporter, bindingContext,
                        config.languageVersionSettings, ignoreErrors,
                        skipBodies = !classBuilderMode.generateBodies
                    )
                }
        }
    }

    companion object {
        fun doCheckCancelled(state: GenerationState) {
            if (state.classBuilderMode.generateBodies) {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            }
        }
    }
}
