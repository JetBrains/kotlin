/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

object JvmBackendFacade {
    fun doGenerateFiles(
        files: Collection<KtFile>,
        state: GenerationState,
        errorHandler: CompilationErrorHandler,
        phaseConfig: PhaseConfig
    ) {
        val psi2ir = Psi2IrTranslator(state.languageVersionSettings, facadeClassGenerator = ::facadeClassGenerator)
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext, extensions = JvmGeneratorExtensions)
        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files)

        doGenerateFilesInternal(state, errorHandler, irModuleFragment, psi2irContext, phaseConfig)
    }

    internal fun doGenerateFilesInternal(
        state: GenerationState,
        errorHandler: CompilationErrorHandler,
        irModuleFragment: IrModuleFragment,
        psi2irContext: GeneratorContext,
        phaseConfig: PhaseConfig
    ) {
        doGenerateFilesInternal(
            state, errorHandler, irModuleFragment, psi2irContext.symbolTable, psi2irContext.sourceManager, phaseConfig
        )
    }

    internal fun doGenerateFilesInternal(
        state: GenerationState,
        errorHandler: CompilationErrorHandler,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        sourceManager: PsiSourceManager,
        phaseConfig: PhaseConfig,
        firMode: Boolean = false
    ) {
        val jvmBackendContext = JvmBackendContext(
            state, sourceManager, irModuleFragment.irBuiltins, irModuleFragment, symbolTable, phaseConfig, firMode
        )
        //TODO
        ExternalDependenciesGenerator(
            irModuleFragment.descriptor,
            symbolTable,
            irModuleFragment.irBuiltins,
            JvmGeneratorExtensions.externalDeclarationOrigin,
            facadeClassGenerator = ::facadeClassGenerator
        ).generateUnboundSymbolsAsDependencies()

        val jvmBackend = JvmBackend(jvmBackendContext)

        for (irFile in irModuleFragment.files) {
            try {
                jvmBackend.lowerFile(irFile)
            } catch (e: Throwable) {
                errorHandler.reportException(e, null) // TODO ktFile.virtualFile.url
            }
        }

        for (irFile in irModuleFragment.files) {
            try {
                jvmBackend.generateLoweredFile(irFile)
                state.afterIndependentPart()
            } catch (e: Throwable) {
                errorHandler.reportException(e, null)
            }
        }
    }

    internal fun facadeClassGenerator(source: DeserializedContainerSource): IrClass? {
        val jvmPackagePartSource = source.safeAs<JvmPackagePartSource>() ?: return null
        val facadeName = jvmPackagePartSource.facadeClassName ?: jvmPackagePartSource.className
        return buildClass {
            origin = IrDeclarationOrigin.FILE_CLASS
            name = facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName()
        }.also {
            it.createParameterDeclarations()
        }
    }
}
