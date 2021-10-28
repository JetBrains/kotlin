/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.analyzer.hasJdkCapability
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.KlibModuleOrigin
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorForNotFoundClasses
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.psi2ir.generators.fragments.FragmentContext
import org.jetbrains.kotlin.psi2ir.generators.generateTypicalIrProviderList
import org.jetbrains.kotlin.psi2ir.preprocessing.SourceDeclarationsPreprocessor
import org.jetbrains.kotlin.resolve.CleanableBindingContext

open class JvmIrCodegenFactory(
    configuration: CompilerConfiguration,
    private val phaseConfig: PhaseConfig?,
    private val externalMangler: JvmDescriptorMangler? = null,
    private val externalSymbolTable: SymbolTable? = null,
    private val jvmGeneratorExtensions: JvmGeneratorExtensionsImpl = JvmGeneratorExtensionsImpl(configuration),
    private val evaluatorFragmentInfoForPsi2Ir: EvaluatorFragmentInfo? = null
) : CodegenFactory {
    data class JvmIrBackendInput(
        val irModuleFragment: IrModuleFragment,
        val symbolTable: SymbolTable,
        val phaseConfig: PhaseConfig?,
        val irProviders: List<IrProvider>,
        val extensions: JvmGeneratorExtensionsImpl,
        val backendExtension: JvmBackendExtension,
        val notifyCodegenStart: () -> Unit
    ) : CodegenFactory.BackendInput

    override fun convertToIr(input: CodegenFactory.IrConversionInput): JvmIrBackendInput {
        val (mangler, symbolTable) =
            if (externalSymbolTable != null) externalMangler!! to externalSymbolTable
            else {
                val mangler = JvmDescriptorMangler(MainFunctionDetector(input.bindingContext, input.languageVersionSettings))
                val symbolTable = SymbolTable(JvmIdSignatureDescriptor(mangler), IrFactoryImpl)
                mangler to symbolTable
            }
        val psi2ir = Psi2IrTranslator(input.languageVersionSettings, Psi2IrConfiguration(input.ignoreErrors))
        val messageLogger = input.configuration[IrMessageLogger.IR_MESSAGE_LOGGER] ?: IrMessageLogger.None
        val psi2irContext = psi2ir.createGeneratorContext(
            input.module,
            input.bindingContext,
            symbolTable,
            jvmGeneratorExtensions,
            fragmentContext = if (evaluatorFragmentInfoForPsi2Ir != null) FragmentContext() else null,
        )
        val pluginExtensions = IrGenerationExtension.getInstances(input.project)

        val stubGenerator =
            DeclarationStubGeneratorImpl(psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns, jvmGeneratorExtensions)
        val frontEndContext = object : TranslationPluginContext {
            override val moduleDescriptor: ModuleDescriptor
                get() = psi2irContext.moduleDescriptor
            override val symbolTable: ReferenceSymbolTable
                get() = symbolTable
            override val typeTranslator: TypeTranslator
                get() = psi2irContext.typeTranslator
            override val irBuiltIns: IrBuiltIns
                get() = psi2irContext.irBuiltIns
        }
        val irLinker = JvmIrLinker(
            psi2irContext.moduleDescriptor,
            messageLogger,
            JvmIrTypeSystemContext(psi2irContext.irBuiltIns),
            symbolTable,
            frontEndContext,
            stubGenerator,
            mangler
        )

        val pluginContext by lazy {
            psi2irContext.run {
                IrPluginContextImpl(
                    moduleDescriptor,
                    bindingContext,
                    languageVersionSettings,
                    symbolTable,
                    typeTranslator,
                    irBuiltIns,
                    irLinker,
                    messageLogger
                )
            }
        }

        SourceDeclarationsPreprocessor(psi2irContext).run(input.files)

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
                irLinker.deserializeIrModuleHeader(
                    it.builtIns.builtInsModule,
                    null,
                    _moduleName = it.builtIns.builtInsModule.name.asString()
                )
            }
            irLinker.deserializeIrModuleHeader(it, kotlinLibrary, _moduleName = it.name.asString())
        }

        val stubGeneratorForMissingClasses = DeclarationStubGeneratorForNotFoundClasses(stubGenerator)
        val irProviders = if (evaluatorFragmentInfoForPsi2Ir != null) {
            listOf(stubGenerator, irLinker, stubGeneratorForMissingClasses)
        } else {
            listOf(irLinker, stubGeneratorForMissingClasses)
        }

        val irModuleFragment =
            psi2ir.generateModuleFragment(
                psi2irContext,
                input.files,
                irProviders,
                pluginExtensions,
                expectDescriptorToSymbol = null,
                fragmentInfo = evaluatorFragmentInfoForPsi2Ir)

        irLinker.postProcess()

        stubGenerator.unboundSymbolGeneration = true

        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(dependencies.flatMap { it.files })

        if (!input.configuration.getBoolean(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT)) {
            val originalBindingContext = input.bindingContext as? CleanableBindingContext
                ?: error("BindingContext should be cleanable in JVM IR to avoid leaking memory: ${input.bindingContext}")
            originalBindingContext.clear()
        }
        return JvmIrBackendInput(
            irModuleFragment,
            symbolTable,
            phaseConfig,
            irProviders,
            jvmGeneratorExtensions,
            JvmBackendExtension.Default,
        ) {}
    }

    private fun ModuleDescriptor.collectAllDependencyModulesTransitively(): List<ModuleDescriptor> {
        val result = LinkedHashSet<ModuleDescriptor>()
        fun collectImpl(descriptor: ModuleDescriptor) {
            val dependencies = descriptor.allDependencyModules
            dependencies.forEach { if (result.add(it)) collectImpl(it) }
        }
        collectImpl(this)
        return result.toList()
    }

    override fun getModuleChunkBackendInput(
        wholeBackendInput: CodegenFactory.BackendInput,
        sourceFiles: Collection<KtFile>,
    ): CodegenFactory.BackendInput {
        wholeBackendInput as JvmIrBackendInput

        val moduleChunk = sourceFiles.toSet()
        val wholeModule = wholeBackendInput.irModuleFragment
        return wholeBackendInput.copy(
            IrModuleFragmentImpl(wholeModule.descriptor, wholeModule.irBuiltins, wholeModule.files.filter { file ->
                file.getKtFile() in moduleChunk
            })
        )
    }

    override fun generateModule(state: GenerationState, input: CodegenFactory.BackendInput) {
        val (irModuleFragment, symbolTable, customPhaseConfig, irProviders, extensions, backendExtension, notifyCodegenStart) =
            input as JvmIrBackendInput
        val irSerializer = if (
            state.configuration.get(JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE) != JvmSerializeIrMode.NONE
        )
            JvmIrSerializerImpl(state.configuration)
        else null
        val phaseConfig = customPhaseConfig ?: PhaseConfig(jvmPhases)
        val context = JvmBackendContext(
            state, irModuleFragment.irBuiltins, irModuleFragment, symbolTable, phaseConfig, extensions, backendExtension, irSerializer,
            notifyCodegenStart,
        )
        /* JvmBackendContext creates new unbound symbols, have to resolve them. */
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

        context.state.factory.registerSourceFiles(irModuleFragment.files.map(IrFile::getKtFile))

        jvmPhases.invokeToplevel(phaseConfig, context, irModuleFragment)

        // TODO: split classes into groups connected by inline calls; call this after every group
        //       and clear `JvmBackendContext.classCodegens`
        state.afterIndependentPart()
    }

    fun generateModuleInFrontendIRMode(
        state: GenerationState,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        extensions: JvmGeneratorExtensionsImpl,
        backendExtension: JvmBackendExtension,
        notifyCodegenStart: () -> Unit = {}
    ) {
        val irProviders = configureBuiltInsAndGenerateIrProvidersInFrontendIRMode(irModuleFragment, symbolTable, extensions)
        generateModule(
            state,
            JvmIrBackendInput(
                irModuleFragment, symbolTable, phaseConfig, irProviders, extensions, backendExtension,
                notifyCodegenStart
            )
        )
    }

    fun configureBuiltInsAndGenerateIrProvidersInFrontendIRMode(
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        extensions: JvmGeneratorExtensionsImpl,
    ): List<IrProvider> {
        return generateTypicalIrProviderList(
            irModuleFragment.descriptor, irModuleFragment.irBuiltins, symbolTable, extensions = extensions
        )
    }
}
