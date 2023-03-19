/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.analyzer.hasJdkCapability
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.phaser.CompilerPhase
import org.jetbrains.kotlin.backend.common.phaser.PhaseConfig
import org.jetbrains.kotlin.backend.common.phaser.invokeToplevel
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.linkerissues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.jvm.codegen.JvmIrIntrinsicExtension
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.backend.jvm.serialization.DisabledIdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.codegen.CodegenFactory
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.linkage.IrProvider
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.descriptors.IrBuiltInsOverDescriptors
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorForNotFoundClasses
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.psi2ir.generators.fragments.FragmentContext
import org.jetbrains.kotlin.psi2ir.preprocessing.SourceDeclarationsPreprocessor
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.utils.IDEAPlatforms
import org.jetbrains.kotlin.utils.IDEAPluginsCompatibilityAPI

open class JvmIrCodegenFactory(
    configuration: CompilerConfiguration,
    private val phaseConfig: PhaseConfig?,
    private val externalMangler: JvmDescriptorMangler? = null,
    private val externalSymbolTable: SymbolTable? = null,
    private val jvmGeneratorExtensions: JvmGeneratorExtensionsImpl = JvmGeneratorExtensionsImpl(configuration),
    private val evaluatorFragmentInfoForPsi2Ir: EvaluatorFragmentInfo? = null,
    private val ideCodegenSettings: IdeCodegenSettings = IdeCodegenSettings(),
) : CodegenFactory {

    @IDEAPluginsCompatibilityAPI(IDEAPlatforms._221, message = "Please migrate to the other constructor", plugins = "Android Studio")
    constructor(
        configuration: CompilerConfiguration,
        phaseConfig: PhaseConfig?,
        externalMangler: JvmDescriptorMangler? = null,
        externalSymbolTable: SymbolTable? = null,
        jvmGeneratorExtensions: JvmGeneratorExtensionsImpl = JvmGeneratorExtensionsImpl(configuration),
        @Suppress("UNUSED_PARAMETER")
        prefixPhases: CompilerPhase<JvmBackendContext, IrModuleFragment, IrModuleFragment>? = null,
        evaluatorFragmentInfoForPsi2Ir: EvaluatorFragmentInfo? = null,
        shouldStubAndNotLinkUnboundSymbols: Boolean = false,
    ) : this(
        configuration,
        phaseConfig,
        externalMangler,
        externalSymbolTable,
        jvmGeneratorExtensions,
        evaluatorFragmentInfoForPsi2Ir,
        IdeCodegenSettings(shouldStubAndNotLinkUnboundSymbols = shouldStubAndNotLinkUnboundSymbols),
    )

    init {
        if (ideCodegenSettings.shouldDeduplicateBuiltInSymbols && !ideCodegenSettings.shouldStubAndNotLinkUnboundSymbols) {
            throw IllegalStateException(
                "`shouldDeduplicateBuiltInSymbols` depends on `shouldStubAndNotLinkUnboundSymbols` being enabled. Deduplication of" +
                        " built-in symbols hasn't been tested without stubbing and there is currently no use case for it without stubbing."
            )
        }
    }

    /**
     * @param shouldStubOrphanedExpectSymbols See [stubOrphanedExpectSymbols].
     * @param shouldDeduplicateBuiltInSymbols See [SymbolTableWithBuiltInsDeduplication].
     */
    data class IdeCodegenSettings(
        val shouldStubAndNotLinkUnboundSymbols: Boolean = false,
        val shouldStubOrphanedExpectSymbols: Boolean = false,
        val shouldDeduplicateBuiltInSymbols: Boolean = false,
    )

    data class JvmIrBackendInput(
        val irModuleFragment: IrModuleFragment,
        val symbolTable: SymbolTable,
        val phaseConfig: PhaseConfig?,
        val irProviders: List<IrProvider>,
        val extensions: JvmGeneratorExtensions,
        val backendExtension: JvmBackendExtension,
        val pluginContext: IrPluginContext,
        val notifyCodegenStart: () -> Unit
    ) : CodegenFactory.BackendInput

    private data class JvmIrCodegenInput(
        override val state: GenerationState,
        val context: JvmBackendContext,
        val module: IrModuleFragment,
        val notifyCodegenStart: () -> Unit,
    ) : CodegenFactory.CodegenInput

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun convertToIr(input: CodegenFactory.IrConversionInput): JvmIrBackendInput {
        val enableIdSignatures =
            input.configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES) ||
                    input.configuration[JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE] != JvmSerializeIrMode.NONE ||
                    input.configuration[JVMConfigurationKeys.KLIB_PATHS, emptyList()].isNotEmpty()
        val (mangler, symbolTable) =
            if (externalSymbolTable != null) externalMangler!! to externalSymbolTable
            else {
                val mangler = JvmDescriptorMangler(MainFunctionDetector(input.bindingContext, input.languageVersionSettings))
                val signaturer =
                    if (enableIdSignatures) JvmIdSignatureDescriptor(mangler)
                    else DisabledIdSignatureDescriptor
                val symbolTable = when {
                    ideCodegenSettings.shouldDeduplicateBuiltInSymbols -> SymbolTableWithBuiltInsDeduplication(signaturer, IrFactoryImpl)
                    else -> SymbolTable(signaturer, IrFactoryImpl)
                }
                mangler to symbolTable
            }
        val messageLogger = input.configuration.irMessageLogger
        val psi2ir = Psi2IrTranslator(
            input.languageVersionSettings,
            Psi2IrConfiguration(
                input.ignoreErrors,
                partialLinkageEnabled = false,
                input.skipBodies
            ),
            messageLogger::checkNoUnboundSymbols
        )
        val psi2irContext = psi2ir.createGeneratorContext(
            input.module,
            input.bindingContext,
            symbolTable,
            jvmGeneratorExtensions,
            fragmentContext = if (evaluatorFragmentInfoForPsi2Ir != null) FragmentContext() else null,
        )

        // Built-ins deduplication must be enabled immediately so that there is no chance for duplicate built-in symbols to occur. For
        // example, the creation of `IrPluginContextImpl` might already lead to duplicate built-in symbols via `BuiltinSymbolsBase`.
        if (symbolTable is SymbolTableWithBuiltInsDeduplication) {
            (psi2irContext.irBuiltIns as? IrBuiltInsOverDescriptors)?.let { symbolTable.bindIrBuiltIns(it) }
        }

        val pluginExtensions = IrGenerationExtension.getInstances(input.project)

        val stubGenerator =
            DeclarationStubGeneratorImpl(
                psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns,
                DescriptorByIdSignatureFinderImpl(psi2irContext.moduleDescriptor, mangler),
                jvmGeneratorExtensions
            )
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
            mangler,
            enableIdSignatures,
        )

        SourceDeclarationsPreprocessor(psi2irContext).run(input.files)

        // The plugin context contains unbound symbols right after construction and has to be
        // instantiated before we resolve unbound symbols and invoke any postprocessing steps.
        val pluginContext = IrPluginContextImpl(
            psi2irContext.moduleDescriptor,
            psi2irContext.bindingContext,
            psi2irContext.languageVersionSettings,
            symbolTable,
            psi2irContext.typeTranslator,
            psi2irContext.irBuiltIns,
            irLinker,
            messageLogger
        )
        if (pluginExtensions.isNotEmpty()) {
            for (extension in pluginExtensions) {
                if (psi2irContext.configuration.generateBodies ||
                    @OptIn(FirIncompatiblePluginAPI::class) extension.shouldAlsoBeAppliedInKaptStubGenerationMode
                ) {
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

        val irProviders = if (ideCodegenSettings.shouldStubAndNotLinkUnboundSymbols) {
            listOf(stubGenerator)
        } else {
            val stubGeneratorForMissingClasses = DeclarationStubGeneratorForNotFoundClasses(stubGenerator)
            listOf(irLinker, stubGeneratorForMissingClasses)
        }

        val irModuleFragment =
            psi2ir.generateModuleFragment(
                psi2irContext,
                input.files,
                irProviders,
                pluginExtensions,
                expectDescriptorToSymbol = null,
                fragmentInfo = evaluatorFragmentInfoForPsi2Ir
            )

        irLinker.postProcess()

        stubGenerator.unboundSymbolGeneration = true

        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(dependencies.flatMap { it.files })

        if (ideCodegenSettings.shouldStubOrphanedExpectSymbols) {
            irModuleFragment.stubOrphanedExpectSymbols(stubGenerator)
        }

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
            pluginContext,
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

    override fun invokeLowerings(state: GenerationState, input: CodegenFactory.BackendInput): CodegenFactory.CodegenInput {
        val (irModuleFragment, symbolTable, customPhaseConfig, irProviders, extensions, backendExtension, irPluginContext, notifyCodegenStart) =
            input as JvmIrBackendInput
        val irSerializer = if (
            state.configuration.get(JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE) != JvmSerializeIrMode.NONE
        )
            JvmIrSerializerImpl(state.configuration)
        else null
        val phases = if (evaluatorFragmentInfoForPsi2Ir != null) jvmFragmentLoweringPhases else jvmLoweringPhases
        val phaseConfig = customPhaseConfig ?: PhaseConfig(phases)
        val context = JvmBackendContext(
            state, irModuleFragment.irBuiltins, symbolTable, phaseConfig, extensions, backendExtension, irSerializer, irPluginContext
        )
        if (evaluatorFragmentInfoForPsi2Ir != null) {
            context.localDeclarationsLoweringData = mutableMapOf()
        }
        val generationExtensions = IrGenerationExtension.getInstances(state.project)
            .mapNotNull { it.getPlatformIntrinsicExtension(context) as? JvmIrIntrinsicExtension }
        val intrinsics by lazy { IrIntrinsicMethods(irModuleFragment.irBuiltins, context.ir.symbols) }
        context.getIntrinsic = { symbol: IrFunctionSymbol ->
            intrinsics.getIntrinsic(symbol) ?: generationExtensions.firstNotNullOfOrNull { it.getIntrinsic(symbol) }
        }
        /* JvmBackendContext creates new unbound symbols, have to resolve them. */
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

        context.state.factory.registerSourceFiles(irModuleFragment.files.map(IrFile::getIoFile))

        phases.invokeToplevel(phaseConfig, context, irModuleFragment)

        return JvmIrCodegenInput(state, context, irModuleFragment, notifyCodegenStart)
    }

    override fun invokeCodegen(input: CodegenFactory.CodegenInput) {
        val (state, context, module, notifyCodegenStart) = input as JvmIrCodegenInput

        fun hasErrors() = (state.diagnosticReporter as? BaseDiagnosticsCollector)?.hasErrors == true

        if (hasErrors()) return

        notifyCodegenStart()
        jvmCodegenPhases.invokeToplevel(PhaseConfig(jvmCodegenPhases), context, module)

        if (hasErrors()) return
        // TODO: split classes into groups connected by inline calls; call this after every group
        //       and clear `JvmBackendContext.classCodegens`
        state.afterIndependentPart()
    }

    fun generateModuleInFrontendIRMode(
        state: GenerationState,
        irModuleFragment: IrModuleFragment,
        symbolTable: SymbolTable,
        irProviders: List<IrProvider>,
        extensions: JvmGeneratorExtensions,
        backendExtension: JvmBackendExtension,
        irPluginContext: IrPluginContext,
        notifyCodegenStart: () -> Unit = {}
    ) {
        generateModule(
            state,
            JvmIrBackendInput(
                irModuleFragment,
                symbolTable,
                phaseConfig,
                irProviders,
                extensions,
                backendExtension,
                irPluginContext,
                notifyCodegenStart
            )
        )
    }
}
