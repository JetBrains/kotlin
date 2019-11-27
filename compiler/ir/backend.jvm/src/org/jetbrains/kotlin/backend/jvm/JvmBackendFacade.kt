/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.codegen.CompilationErrorHandler
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

object JvmBackendFacade {
    fun doGenerateFiles(
        files: Collection<KtFile>,
        state: GenerationState,
        errorHandler: CompilationErrorHandler,
        phaseConfig: PhaseConfig
    ) {
        val psi2ir = Psi2IrTranslator(state.languageVersionSettings)
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext, extensions = JvmGeneratorExtensions)
        val extensions = JvmStubGeneratorExtensions()

        for (extension in IrGenerationExtension.getInstances(state.project)) {
            psi2ir.addPostprocessingStep { module ->
                extension.generate(
                    module,
                    IrPluginContext(
                        psi2irContext.moduleDescriptor,
                        psi2irContext.bindingContext,
                        psi2irContext.languageVersionSettings,
                        psi2irContext.symbolTable,
                        psi2irContext.typeTranslator,
                        psi2irContext.irBuiltIns
                    )
                )
            }
        }

        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, stubGeneratorExtensions = extensions)
        doGenerateFilesInternal(
            state, errorHandler, irModuleFragment, psi2irContext.symbolTable, psi2irContext.sourceManager, phaseConfig, extensions
        )
    }

    internal fun doGenerateFilesInternal(
        state: GenerationState,
        errorHandler: CompilationErrorHandler,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        sourceManager: PsiSourceManager,
        phaseConfig: PhaseConfig,
        extensions: JvmStubGeneratorExtensions = JvmStubGeneratorExtensions()
    ) {
        val context = JvmBackendContext(
            state, sourceManager, irModuleFragment.irBuiltins, irModuleFragment, symbolTable, phaseConfig
        )
        state.irBasedMapAsmMethod = { descriptor ->
            context.methodSignatureMapper.mapAsmMethod(context.referenceFunction(descriptor).owner)
        }
        state.mapInlineClass = { descriptor ->
            context.typeMapper.mapType(context.referenceClass(descriptor).defaultType)
        }

        ExternalDependenciesGenerator(
            irModuleFragment.descriptor,
            symbolTable,
            irModuleFragment.irBuiltins,
            extensions = extensions
        ).generateUnboundSymbolsAsDependencies()

        context.classNameOverride = extensions.classNameOverride

        try {
            JvmLower(context).lower(irModuleFragment)
        } catch (e: Throwable) {
            errorHandler.reportException(e, null)
        }

        for (generateMultifileFacade in listOf(true, false)) {
            for (irFile in irModuleFragment.files) {
                // Generate multifile facades first, to compute and store JVM signatures of const properties which are later used
                // when serializing metadata in the multifile parts.
                // TODO: consider dividing codegen itself into separate phases (bytecode generation, metadata serialization) to avoid this
                val isMultifileFacade = irFile.fileEntry is MultifileFacadeFileEntry
                if (isMultifileFacade != generateMultifileFacade) continue

                try {
                    for (loweredClass in irFile.declarations) {
                        if (loweredClass !is IrClass) {
                            throw AssertionError("File-level declaration should be IrClass after JvmLower, got: " + loweredClass.render())
                        }

                        ClassCodegen.generate(loweredClass, context)
                    }
                    state.afterIndependentPart()
                } catch (e: Throwable) {
                    errorHandler.reportException(e, null) // TODO ktFile.virtualFile.url
                }
            }
        }
    }
}
