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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator

object JvmBackendFacade {
    fun compileCorrectFiles(state: GenerationState, errorHandler: CompilationErrorHandler) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        state.beforeCompile()
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        doGenerateFiles(state.files, state, errorHandler)
    }

    fun doGenerateFiles(files: Collection<KtFile>, state: GenerationState, errorHandler: CompilationErrorHandler) {
        // TODO multifile classes support

        val psi2ir = Psi2IrTranslator()
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext)
        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files)

        val jvmBackendContext = JvmBackendContext(
                state, psi2irContext.sourceManager, psi2irContext.irBuiltIns, irModuleFragment, psi2irContext.symbolTable
        )
        val jvmBackend = JvmBackend(jvmBackendContext)

        for (irFile in irModuleFragment.files) {
            try {
                jvmBackend.generateFile(irFile)
                state.afterIndependentPart()
            }
            catch (e: Throwable) {
                errorHandler.reportException(e, null) // TODO ktFile.virtualFile.url
            }
        }
    }

}
