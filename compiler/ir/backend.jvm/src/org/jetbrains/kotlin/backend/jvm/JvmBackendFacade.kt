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
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext

object JvmBackendFacade {

    fun doGenerateFiles(files: Collection<KtFile>, state: GenerationState, errorHandler: CompilationErrorHandler) {
        val psi2ir = Psi2IrTranslator()
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext)
        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files)

        doGenerateFilesInternal(state, errorHandler, irModuleFragment, psi2irContext)
    }

    internal fun doGenerateFilesInternal(
            state: GenerationState,
            errorHandler: CompilationErrorHandler,
            irModuleFragment: IrModuleFragment,
            psi2irContext: GeneratorContext
    ) {
        val jvmBackendContext = JvmBackendContext(
                state, psi2irContext.sourceManager, psi2irContext.irBuiltIns, irModuleFragment, psi2irContext.symbolTable
        )
        //TODO
        ExternalDependenciesGenerator(psi2irContext.symbolTable, psi2irContext.irBuiltIns).generateUnboundSymbolsAsDependencies(irModuleFragment)

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
