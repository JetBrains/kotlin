/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.DescriptorBasedClassCodegen
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.backend.jvm.serialization.EmptyLoggingContext
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
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
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext, JvmNameProvider, extensions = extensions)
        val pluginExtensions = IrGenerationExtension.getInstances(state.project)
        val functionFactory = IrFunctionFactory(psi2irContext.irBuiltIns, psi2irContext.symbolTable)
        psi2irContext.irBuiltIns.functionFactory = functionFactory

        val stubGenerator = DeclarationStubGenerator(
            psi2irContext.moduleDescriptor, psi2irContext.symbolTable, psi2irContext.irBuiltIns.languageVersionSettings, extensions
        )
        val irLinker = JvmIrLinker(
            psi2irContext.moduleDescriptor,
            EmptyLoggingContext,
            psi2irContext.irBuiltIns,
            psi2irContext.symbolTable,
            functionFactory,
            stubGenerator,
            mangler
        )

        val pluginContext by lazy {
            psi2irContext.run {
                val symbols = BuiltinSymbolsBase(irBuiltIns, moduleDescriptor.builtIns, symbolTable.lazyWrapper)
                IrPluginContextImpl(
                    moduleDescriptor, bindingContext, languageVersionSettings, symbolTable, typeTranslator, irBuiltIns, irLinker, symbols
                )
            }
        }

        for (extension in pluginExtensions) {
            psi2ir.addPostprocessingStep { module ->
                val old = stubGenerator.unboundSymbolGeneration
                try {
                    stubGenerator.unboundSymbolGeneration = true
                    extension.generate(module, pluginContext)
                } finally {
                    stubGenerator.unboundSymbolGeneration = old
                }
            }
        }

        val dependencies = psi2irContext.moduleDescriptor.allDependencyModules.map {
            val kotlinLibrary = (it.getCapability(KlibModuleOrigin.CAPABILITY) as? DeserializedKlibModuleOrigin)?.library
            irLinker.deserializeIrModuleHeader(it, kotlinLibrary)
        }
        val irProviders = listOf(irLinker)

        stubGenerator.setIrProviders(irProviders)

        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, irProviders, pluginExtensions, expectDescriptorToSymbol = null)
        irLinker.postProcess()

        stubGenerator.unboundSymbolGeneration = true

        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(dependencies.flatMap { it.files })

        doGenerateFilesInternal(
            state, irModuleFragment, psi2irContext.symbolTable, psi2irContext.sourceManager, phaseConfig, irProviders, extensions
        ) { irClass, context, parentFunction -> DescriptorBasedClassCodegen(irClass, context, parentFunction) }
    }

    internal fun doGenerateFilesInternal(
        state: GenerationState,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        sourceManager: PsiSourceManager,
        phaseConfig: PhaseConfig,
        irProviders: List<IrProvider>,
        extensions: JvmGeneratorExtensions,
        createCodegen: (IrClass, JvmBackendContext, IrFunction?) -> ClassCodegen,
    ) {
        val context = JvmBackendContext(
            state, sourceManager, irModuleFragment.irBuiltins, irModuleFragment,
            symbolTable, phaseConfig, extensions.classNameOverride, createCodegen
        )
        /* JvmBackendContext creates new unbound symbols, have to resolve them. */
        ExternalDependenciesGenerator(symbolTable, irProviders, state.languageVersionSettings).generateUnboundSymbolsAsDependencies()

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
                        context.getClassCodegen(loweredClass).generate()
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
