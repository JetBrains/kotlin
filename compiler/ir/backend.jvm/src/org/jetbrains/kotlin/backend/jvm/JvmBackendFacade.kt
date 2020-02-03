/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.ir.backend.jvm.serialization.EmptyLoggingContext
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.PsiSourceManager

object JvmBackendFacade {
    fun doGenerateFiles(files: Collection<KtFile>, state: GenerationState, phaseConfig: PhaseConfig) {
        val extensions = JvmGeneratorExtensions()
        val mangler = JvmManglerDesc(MainFunctionDetector(state.bindingContext, state.languageVersionSettings))
        val signaturer = JvmIdSignatureDescriptor(mangler)
        val psi2ir = Psi2IrTranslator(state.languageVersionSettings, signaturer = signaturer)
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext, extensions = extensions)

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

        val stubGenerator = DeclarationStubGenerator(
            psi2irContext.moduleDescriptor, psi2irContext.symbolTable, psi2irContext.irBuiltIns.languageVersionSettings, extensions
        )
        val deserializer = JvmIrLinker(
            EmptyLoggingContext, psi2irContext.irBuiltIns, psi2irContext.symbolTable
        )
        psi2irContext.moduleDescriptor.allDependencyModules.filter { it.getCapability(KlibModuleOrigin.CAPABILITY) != null }.forEach {
            deserializer.deserializeIrModuleHeader(it)
        }
        val irProviders = listOf(deserializer, stubGenerator)
        stubGenerator.setIrProviders(irProviders)

        val irModuleFragment = psi2ir.generateModuleFragment(
            psi2irContext, files,
            irProviders = irProviders,
            expectDescriptorToSymbol = null
        )
        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(deserializer.getAllIrFiles())

        doGenerateFilesInternal(
            state, irModuleFragment, psi2irContext.symbolTable, psi2irContext.sourceManager, phaseConfig, irProviders, extensions
        )
    }

    internal fun doGenerateFilesInternal(
        state: GenerationState,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        sourceManager: PsiSourceManager,
        phaseConfig: PhaseConfig,
        irProviders: List<IrProvider>,
        extensions: JvmGeneratorExtensions
    ) {
        val context = JvmBackendContext(
            state, sourceManager, irModuleFragment.irBuiltins, irModuleFragment, symbolTable, phaseConfig, extensions.classNameOverride
        )
        /* JvmBackendContext creates new unbound symbols, have to resolve them. */
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

        state.irBasedMapAsmMethod = { descriptor ->
            context.methodSignatureMapper.mapAsmMethod(context.referenceFunction(descriptor).owner)
        }
        state.mapInlineClass = { descriptor ->
            context.typeMapper.mapType(context.referenceClass(descriptor).defaultType)
        }

        JvmLower(context).lower(irModuleFragment)

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
                        ClassCodegen.getOrCreate(loweredClass, context).generate()
                    }
                } catch (e: Throwable) {
                    CodegenUtil.reportBackendException(e, "code generation", irFile.fileEntry.name)
                }
            }
        }
        // TODO: split classes into groups connected by inline calls; call this after every group
        //       and clear `JvmBackendContext.classCodegens`
        state.afterIndependentPart()
    }
}
