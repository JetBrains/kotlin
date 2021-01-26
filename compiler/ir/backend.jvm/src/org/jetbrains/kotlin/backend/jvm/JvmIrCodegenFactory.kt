/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.analyzer.hasJdkCapability
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.BuiltinSymbolsBase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.lower.MultifileFacadeFileEntry
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.backend.jvm.serialization.EmptyLoggingContext
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrFunctionFactory
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext

class JvmIrCodegenFactory(private val phaseConfig: PhaseConfig) : CodegenFactory {
    data class JvmIrBackendInput(
        val state: GenerationState,
        val irModuleFragment: IrModuleFragment,
        val symbolTable: SymbolTable,
        val sourceManager: PsiSourceManager,
        val phaseConfig: PhaseConfig,
        val irProviders: List<IrProvider>,
        val extensions: JvmGeneratorExtensions,
        val backendExtension: JvmBackendExtension,
    )

    override fun generateModule(state: GenerationState, files: Collection<KtFile>) {
        val input = convertToIr(state, files)
        doGenerateFilesInternal(input)
    }

    @JvmOverloads
    fun convertToIr(state: GenerationState, files: Collection<KtFile>, ignoreErrors: Boolean = false): JvmIrBackendInput {
        val extensions = JvmGeneratorExtensions()
        val mangler = JvmManglerDesc(MainFunctionDetector(state.bindingContext, state.languageVersionSettings))
        val psi2ir = Psi2IrTranslator(state.languageVersionSettings, Psi2IrConfiguration(ignoreErrors))
        val symbolTable = SymbolTable(JvmIdSignatureDescriptor(mangler), IrFactoryImpl, JvmNameProvider)
        val psi2irContext = psi2ir.createGeneratorContext(state.module, state.bindingContext, symbolTable, extensions)
        val pluginExtensions = IrGenerationExtension.getInstances(state.project)
        val functionFactory = IrFunctionFactory(psi2irContext.irBuiltIns, symbolTable)
        psi2irContext.irBuiltIns.functionFactory = functionFactory

        val stubGenerator = DeclarationStubGenerator(
            psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns.languageVersionSettings, extensions
        )
        val frontEndContext = object : TranslationPluginContext {
            override val moduleDescriptor: ModuleDescriptor
                get() = psi2irContext.moduleDescriptor
            override val bindingContext: BindingContext
                get() = psi2irContext.bindingContext
            override val symbolTable: ReferenceSymbolTable
                get() = symbolTable
            override val typeTranslator: TypeTranslator
                get() = psi2irContext.typeTranslator
            override val irBuiltIns: IrBuiltIns
                get() = psi2irContext.irBuiltIns
        }
        val irLinker = JvmIrLinker(
            psi2irContext.moduleDescriptor,
            EmptyLoggingContext,
            psi2irContext.irBuiltIns,
            symbolTable,
            functionFactory,
            frontEndContext,
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

        val dependencies = psi2irContext.moduleDescriptor.collectAllDependencyModulesTransitively().map {
            val kotlinLibrary = (it.getCapability(KlibModuleOrigin.CAPABILITY) as? DeserializedKlibModuleOrigin)?.library
            if (it.hasJdkCapability) {
                // For IDE environment only, i.e. when compiling for debugger
                // Deserializer for built-ins module should exist because built-in types returned from SDK belong to that module,
                // but JDK's built-ins module might not be in current module's dependencies
                // We have to ensure that deserializer for built-ins module is created
                irLinker.deserializeIrModuleHeader(it.builtIns.builtInsModule, null)
            }
            irLinker.deserializeIrModuleHeader(it, kotlinLibrary)
        }
        val irProviders = listOf(irLinker)

        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, irProviders, pluginExtensions, expectDescriptorToSymbol = null)
        irLinker.postProcess()

        stubGenerator.unboundSymbolGeneration = true

        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(dependencies.flatMap { it.files })

        if (!state.configuration.getBoolean(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT)) {
            val originalBindingContext = state.originalFrontendBindingContext as? CleanableBindingContext
                ?: error("BindingContext should be cleanable in JVM IR to avoid leaking memory: ${state.originalFrontendBindingContext}")
            originalBindingContext.clear()
        }
        return JvmIrBackendInput(
            state,
            irModuleFragment,
            symbolTable,
            psi2irContext.sourceManager,
            phaseConfig,
            irProviders,
            extensions,
            JvmBackendExtension.Default,
        )
    }

    private fun ModuleDescriptor.collectAllDependencyModulesTransitively(): List<ModuleDescriptor> {
        val result = LinkedHashSet<ModuleDescriptor>()
        fun collectImpl(descriptor: ModuleDescriptor) {
            val dependencies = descriptor.allDependencyModules
            dependencies.forEach { if (it !in result) collectImpl(it) }
            result += dependencies
        }
        collectImpl(this)
        return result.toList()
    }

    fun doGenerateFilesInternal(input: JvmIrBackendInput) {
        val (state, irModuleFragment, symbolTable, sourceManager, phaseConfig, irProviders, extensions, backendExtension) = input
        val context = JvmBackendContext(
            state, sourceManager, irModuleFragment.irBuiltins, irModuleFragment,
            symbolTable, phaseConfig, extensions, backendExtension
        )
        /* JvmBackendContext creates new unbound symbols, have to resolve them. */
        ExternalDependenciesGenerator(symbolTable, irProviders, state.languageVersionSettings).generateUnboundSymbolsAsDependencies()

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

    fun generateModuleInFrontendIRMode(
        state: GenerationState,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        sourceManager: PsiSourceManager,
        extensions: JvmGeneratorExtensions,
        backendExtension: JvmBackendExtension,
    ) {
        val irProviders = configureBuiltInsAndgenerateIrProvidersInFrontendIRMode(irModuleFragment, symbolTable, extensions)
        doGenerateFilesInternal(
            JvmIrBackendInput(state, irModuleFragment, symbolTable, sourceManager, phaseConfig, irProviders, extensions, backendExtension)
        )
    }

    fun configureBuiltInsAndgenerateIrProvidersInFrontendIRMode(
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        extensions: JvmGeneratorExtensions
    ): List<IrProvider> {
        irModuleFragment.irBuiltins.functionFactory = IrFunctionFactory(irModuleFragment.irBuiltins, symbolTable)
        return generateTypicalIrProviderList(
            irModuleFragment.descriptor, irModuleFragment.irBuiltins, symbolTable, extensions = extensions
        )
    }
}
