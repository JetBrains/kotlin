/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
        ExternalDependenciesGenerator(
            irModuleFragment.descriptor,
            psi2irContext.symbolTable,
            psi2irContext.irBuiltIns
        ).generateUnboundSymbolsAsDependencies(irModuleFragment)

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
