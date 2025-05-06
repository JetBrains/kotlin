/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.backend.common.ir.isBytecodeGenerationSuppressed
import org.jetbrains.kotlin.backend.common.ir.isJvmBuiltin
import org.jetbrains.kotlin.backend.common.linkage.issues.checkNoUnboundSymbols
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.EnumEntriesIntrinsicMappingsCacheImpl
import org.jetbrains.kotlin.backend.jvm.codegen.JvmIrIntrinsicExtension
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.backend.jvm.serialization.DisabledIdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.codegen.addCompiledPartsAndSort
import org.jetbrains.kotlin.codegen.loadCompiledModule
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrLinker
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.Psi2IrConfiguration
import org.jetbrains.kotlin.psi2ir.Psi2IrTranslator
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorForNotFoundClasses
import org.jetbrains.kotlin.psi2ir.generators.DeclarationStubGeneratorImpl
import org.jetbrains.kotlin.psi2ir.generators.fragments.EvaluatorFragmentInfo
import org.jetbrains.kotlin.psi2ir.generators.fragments.FragmentContext
import org.jetbrains.kotlin.psi2ir.preprocessing.SourceDeclarationsPreprocessor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.CleanableBindingContext
import org.jetbrains.kotlin.serialization.StringTableImpl
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JvmIrCodegenFactory(
    configuration: CompilerConfiguration,
    private val externalMangler: JvmDescriptorMangler? = null,
    private val externalSymbolTable: SymbolTable? = null,
    private val jvmGeneratorExtensions: JvmGeneratorExtensionsImpl = JvmGeneratorExtensionsImpl(configuration),
    private val evaluatorFragmentInfoForPsi2Ir: EvaluatorFragmentInfo? = null,
    private val ideCodegenSettings: IdeCodegenSettings = IdeCodegenSettings(),
) {
    /**
     * @param shouldStubAndNotLinkUnboundSymbols
     * must be `true` only if current compilation is done in the context of the "Evaluate Expression"
     * process in the debugger or "Android LiveEdit plugin".
     * When enabled, this option disables the linkage process and generates stubs for all unbound symbols.
     * @param shouldStubOrphanedExpectSymbols See [stubOrphanedExpectSymbols].
     * @param shouldReferenceUndiscoveredExpectSymbols See [referenceUndiscoveredExpectSymbols].
     * @param shouldDeduplicateBuiltInSymbols See [SymbolTableWithBuiltInsDeduplication].
     * @param doNotLoadDependencyModuleHeaders
     * must be `true` only if current compilation is done in the context of the "Evaluate Expression" process in the debugger.
     * When enabled, this option disables all compiler plugins.
     */
    data class IdeCodegenSettings(
        val shouldStubAndNotLinkUnboundSymbols: Boolean = false,
        val shouldStubOrphanedExpectSymbols: Boolean = false,
        val shouldReferenceUndiscoveredExpectSymbols: Boolean = false,
        val shouldDeduplicateBuiltInSymbols: Boolean = false,
        val doNotLoadDependencyModuleHeaders: Boolean = false,
    ) {
        init {
            if (shouldDeduplicateBuiltInSymbols && !shouldStubAndNotLinkUnboundSymbols) {
                throw IllegalStateException(
                    "`shouldDeduplicateBuiltInSymbols` depends on `shouldStubAndNotLinkUnboundSymbols` being enabled. Deduplication of" +
                            " built-in symbols hasn't been tested without stubbing and there is currently no use case for it without stubbing."
                )
            }
        }
    }

    data class BackendInput(
        val irModuleFragment: IrModuleFragment,
        val irBuiltIns: IrBuiltIns,
        val symbolTable: SymbolTable,
        val irProviders: List<IrProvider>,
        val extensions: JvmGeneratorExtensions,
        val backendExtension: JvmBackendExtension,
        val pluginContext: IrPluginContext?,
    )

    data class CodegenInput(
        val state: GenerationState,
        val context: JvmBackendContext,
        val module: IrModuleFragment,
        val allBuiltins: List<IrFile>,
        val intrinsicExtensions: List<JvmIrIntrinsicExtension>,
    )

    /**
     * If the extension is marked with this marker, it is applied even on expression evaluation in the IDE
     *
     * see [convertToIr] for implementation details
     */
    interface IrGeneratorExtensionMarkerForExpressionEvaluation

    fun convertAndGenerate(files: Collection<KtFile>, state: GenerationState, bindingContext: BindingContext) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        val backendInput = convertToIr(state, files, bindingContext)
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        generateModule(state, backendInput)
    }

    fun convertToIr(state: GenerationState, files: Collection<KtFile>, bindingContext: BindingContext): BackendInput = with(state) {
        convertToIr(
            project, files, configuration, module, diagnosticReporter, bindingContext, config.languageVersionSettings, ignoreErrors,
            skipBodies = !classBuilderMode.generateBodies
        )
    }

    private val Project.filteredExtensions: List<IrGenerationExtension>
        get() = IrGenerationExtension.getInstances(this)
            .filter { !ideCodegenSettings.doNotLoadDependencyModuleHeaders || it is IrGeneratorExtensionMarkerForExpressionEvaluation }


    @OptIn(ObsoleteDescriptorBasedAPI::class)
    fun convertToIr(
        project: Project,
        files: Collection<KtFile>,
        configuration: CompilerConfiguration,
        module: ModuleDescriptor,
        diagnosticReporter: DiagnosticReporter,
        bindingContext: BindingContext,
        languageVersionSettings: LanguageVersionSettings,
        ignoreErrors: Boolean,
        skipBodies: Boolean,
    ): BackendInput {
        val enableIdSignatures =
            configuration.getBoolean(JVMConfigurationKeys.LINK_VIA_SIGNATURES) ||
                    configuration[JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE] != JvmSerializeIrMode.NONE ||
                    configuration[JVMConfigurationKeys.KLIB_PATHS, emptyList()].isNotEmpty()
        val (mangler, symbolTable) =
            if (externalSymbolTable != null) externalMangler!! to externalSymbolTable
            else {
                val mangler = JvmDescriptorMangler(MainFunctionDetector(bindingContext, languageVersionSettings))
                val signaturer =
                    if (enableIdSignatures) JvmIdSignatureDescriptor(mangler)
                    else DisabledIdSignatureDescriptor
                val symbolTable = when {
                    ideCodegenSettings.shouldDeduplicateBuiltInSymbols -> SymbolTableWithBuiltInsDeduplication(signaturer, IrFactoryImpl)
                    else -> SymbolTable(signaturer, IrFactoryImpl)
                }
                mangler to symbolTable
            }
        val messageCollector = configuration.messageCollector
        val psi2ir = Psi2IrTranslator(
            languageVersionSettings,
            Psi2IrConfiguration(ignoreErrors, partialLinkageEnabled = false, skipBodies),
            messageCollector::checkNoUnboundSymbols
        )
        val psi2irContext = psi2ir.createGeneratorContext(
            module,
            bindingContext,
            symbolTable,
            jvmGeneratorExtensions,
            fragmentContext = if (evaluatorFragmentInfoForPsi2Ir != null) FragmentContext() else null,
        )

        // Built-ins deduplication must be enabled immediately so that there is no chance for duplicate built-in symbols to occur. For
        // example, the creation of `IrPluginContextImpl` might already lead to duplicate built-in symbols via `BuiltinSymbolsBase`.
        if (symbolTable is SymbolTableWithBuiltInsDeduplication) {
            symbolTable.bindBuiltIns(psi2irContext.moduleDescriptor.builtIns)
        }

        val stubGenerator =
            DeclarationStubGeneratorImpl(
                psi2irContext.moduleDescriptor, symbolTable, psi2irContext.irBuiltIns,
                DescriptorByIdSignatureFinderImpl(psi2irContext.moduleDescriptor, mangler),
                jvmGeneratorExtensions
            )
        val irLinker = JvmIrLinker(
            psi2irContext.moduleDescriptor,
            messageCollector,
            JvmIrTypeSystemContext(psi2irContext.irBuiltIns),
            symbolTable,
            stubGenerator,
            mangler,
            enableIdSignatures,
        )

        SourceDeclarationsPreprocessor(psi2irContext).run(files)

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
            messageCollector,
            diagnosticReporter
        )
        for (extension in project.filteredExtensions) {
            if (!psi2irContext.configuration.generateBodies &&
                !@OptIn(FirIncompatiblePluginAPI::class) extension.shouldAlsoBeAppliedInKaptStubGenerationMode
            ) continue

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

        val dependencies = if (ideCodegenSettings.doNotLoadDependencyModuleHeaders) {
            emptyList()
        } else {
            psi2irContext.moduleDescriptor.collectAllDependencyModulesTransitively().map {
                val kotlinLibrary = (it.getCapability(KlibModuleOrigin.CAPABILITY) as? DeserializedKlibModuleOrigin)?.library
                irLinker.deserializeIrModuleHeader(it, kotlinLibrary, _moduleName = it.name.asString())
            }
        }

        val irProviders = if (ideCodegenSettings.shouldStubAndNotLinkUnboundSymbols) {
            listOf(stubGenerator)
        } else {
            val stubGeneratorForMissingClasses = DeclarationStubGeneratorForNotFoundClasses(stubGenerator)
            listOf(irLinker, stubGeneratorForMissingClasses)
        }

        if (ideCodegenSettings.shouldReferenceUndiscoveredExpectSymbols) {
            symbolTable.referenceUndiscoveredExpectSymbols(files, bindingContext)
        }

        val irModuleFragment = psi2ir.generateModuleFragment(psi2irContext, files, irProviders, evaluatorFragmentInfoForPsi2Ir)

        irLinker.postProcess(inOrAfterLinkageStep = true)
        irLinker.clear()

        stubGenerator.unboundSymbolGeneration = true

        // We need to compile all files we reference in Klibs
        irModuleFragment.files.addAll(dependencies.flatMap { it.files })

        if (ideCodegenSettings.shouldStubOrphanedExpectSymbols) {
            irModuleFragment.stubOrphanedExpectSymbols(stubGenerator)
        }

        if (!configuration.getBoolean(JVMConfigurationKeys.DO_NOT_CLEAR_BINDING_CONTEXT)) {
            if (bindingContext !is CleanableBindingContext) {
                error("BindingContext should be cleanable in JVM IR to avoid leaking memory: $bindingContext")
            }
            bindingContext.clear()
        }
        return BackendInput(
            irModuleFragment,
            psi2irContext.irBuiltIns,
            symbolTable,
            irProviders,
            jvmGeneratorExtensions,
            JvmBackendExtension.Default,
            pluginContext,
        )
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

    // Extracts a part of the BackendInput which corresponds only to the specified source files.
    // This is needed to support cyclic module dependencies, which are allowed in JPS, where frontend and psi2ir is run on sources of all
    // modules combined, and then backend is run on each individual module.
    fun getModuleChunkBackendInput(wholeBackendInput: BackendInput, sourceFiles: Collection<KtFile>): BackendInput {
        val moduleChunk = sourceFiles.toSet()
        val wholeModule = wholeBackendInput.irModuleFragment
        val moduleCopy = IrModuleFragmentImpl(wholeModule.descriptor)
        wholeModule.files.filterTo(moduleCopy.files) { file ->
            file.getKtFile() in moduleChunk
        }
        return wholeBackendInput.copy(moduleCopy)
    }

    fun generateModule(state: GenerationState, input: BackendInput) {
        val result = invokeLowerings(state, input)
        invokeCodegen(result)
    }

    fun invokeLowerings(state: GenerationState, input: BackendInput): CodegenInput {
        val (irModuleFragment, irBuiltIns, symbolTable, irProviders, extensions, backendExtension, irPluginContext) =
            input
        val irSerializer = if (
            state.configuration.get(JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE) != JvmSerializeIrMode.NONE
        )
            JvmIrSerializerImpl(state.configuration)
        else null
        val context = JvmBackendContext(
            state, irBuiltIns, symbolTable, extensions,
            backendExtension, irSerializer, JvmIrDeserializerImpl(), irProviders, irPluginContext
        )
        if (evaluatorFragmentInfoForPsi2Ir != null) {
            // In K1 CodeFragment metadata is attributed to IrClass, but in K2 it is attributed IrFile
            val generatedClass = if (context.state.configuration.useFir) {
                irModuleFragment.files.flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .single { it.metadata is MetadataSource.CodeFragment }
            } else {
                val fragmentFile = irModuleFragment.files.single { it.metadata is MetadataSource.CodeFragment }
                fragmentFile.declarations.single() as IrClass
            }

            @OptIn(ObsoleteDescriptorBasedAPI::class)
            val evaluationEntryPoint = generatedClass.functions.single { it.descriptor == evaluatorFragmentInfoForPsi2Ir.methodDescriptor }
            context.evaluatorData =
                JvmEvaluatorData(
                    JvmBackendContext.SharedLocalDeclarationsData(),
                    evaluationEntryPoint,
                    evaluatorFragmentInfoForPsi2Ir.typeArgumentsMap
                )
        }
        val generationExtensions = state.project.filteredExtensions
            .mapNotNull { it.getPlatformIntrinsicExtension(context) as? JvmIrIntrinsicExtension }
        val intrinsics by lazy { IrIntrinsicMethods(irBuiltIns, context.symbols) }
        context.getIntrinsic = { symbol: IrFunctionSymbol ->
            intrinsics.getIntrinsic(symbol) ?: generationExtensions.firstNotNullOfOrNull { it.getIntrinsic(symbol) }
        }

        context.enumEntriesIntrinsicMappingsCache = EnumEntriesIntrinsicMappingsCacheImpl(context, generationExtensions)

        /* JvmBackendContext creates new unbound symbols, have to resolve them. */
        ExternalDependenciesGenerator(symbolTable, irProviders).generateUnboundSymbolsAsDependencies()

        context.state.factory.registerSourceFiles(irModuleFragment.files.map(IrFile::getIoFile))

        val allBuiltins = irModuleFragment.files.filter { it.isJvmBuiltin }
        irModuleFragment.files.removeIf { it.isBytecodeGenerationSuppressed }

        val engine = PhaseEngine(state.configuration.phaseConfig ?: PhaseConfig(), PhaserState(), context)
        for (phase in jvmLoweringPhases) {
            engine.runPhase(phase, irModuleFragment)
        }

        return CodegenInput(state, context, irModuleFragment, allBuiltins, generationExtensions)
    }

    fun invokeCodegen(input: CodegenInput) {
        val (state, context, module, allBuiltins, intrinsicExtensions) = input

        fun hasErrors() = (state.diagnosticReporter as? BaseDiagnosticsCollector)?.hasErrors == true

        if (hasErrors()) return

        val nThreads = context.configuration.get(CommonConfigurationKeys.PARALLEL_BACKEND_THREADS) ?: 1
        val executor = if (nThreads > 1) Executors.newFixedThreadPool(nThreads) else null

        // Generate multifile facades first, to compute and store JVM signatures of const properties which are later used
        // when serializing metadata in the multifile parts.
        // TODO: consider dividing codegen itself into separate phases (bytecode generation, metadata serialization) to avoid this
        for (generateMultifileFacades in listOf(true, false)) {
            if (executor != null) {
                val taskPerFile = module.files.map { irFile ->
                    CompletableFuture.runAsync(
                        {
                            generateFile(context, irFile, intrinsicExtensions, generateMultifileFacades)
                        },
                        executor
                    )
                }
                CompletableFuture.allOf(*taskPerFile.toTypedArray()).get()
            } else {
                for (irFile in module.files) {
                    generateFile(context, irFile, intrinsicExtensions, generateMultifileFacades)
                }
            }
        }
        executor?.shutdown()
        executor?.awaitTermination(1, TimeUnit.DAYS) // Wait long enough

        context.enumEntriesIntrinsicMappingsCache.generateMappingsClasses()

        if (hasErrors()) return

        generateModuleMetadata(input.context)
        if (state.config.languageVersionSettings.getFlag(JvmAnalysisFlags.outputBuiltinsMetadata)) {
            require(state.config.useFir) { "Stdlib is expected to be compiled by K2" }
            serializeBuiltinsMetadata(allBuiltins, context)
        }

        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
        state.factory.done()
    }

    private fun generateFile(
        context: JvmBackendContext,
        file: IrFile,
        intrinsicExtensions: List<JvmIrIntrinsicExtension>,
        generateMultifileFacades: Boolean,
    ): IrFile {
        val isMultifileFacade = file.fileEntry is MultifileFacadeFileEntry
        if (isMultifileFacade == generateMultifileFacades) {
            for (loweredClass in file.declarations) {
                if (loweredClass !is IrClass) {
                    throw AssertionError("File-level declaration should be IrClass after JvmLower: " + loweredClass.render())
                }
                ClassCodegen.getOrCreate(loweredClass, context, intrinsicExtensions).generate()
            }
        }
        return file
    }

    private fun serializeBuiltinsMetadata(allBuiltins: List<IrFile>, context: JvmBackendContext) {
        val serializer = context.backendExtension.createBuiltinsSerializer()
        val serializedPackages = serializer.serialize(allBuiltins.map { it.metadata as MetadataSource.File })
        require(serializedPackages.map { it.first }.toSet() == BUILT_INS_PACKAGE_FQ_NAMES) { "Unexpected set of builtin packages" }
        for ((packageName, serialized) in serializedPackages) {
            context.state.factory.addSerializedBuiltinsPackageMetadata(
                BuiltInSerializerProtocol.getBuiltInsFilePath(packageName),
                serialized
            )
        }
    }

    private fun generateModuleMetadata(backendContext: JvmBackendContext) {
        val builder = JvmModuleProtoBuf.Module.newBuilder()
        val stringTable = StringTableImpl()

        backendContext.state.loadCompiledModule()?.moduleData?.run {
            // In incremental compilation scenario, we might already have some serialized optionalAnnotations from the previous run
            // In this case, we first initialize string table with the serialized one
            // See jps/jps-plugin/testData/incremental/multiModule/multiplatform/custom/modifyOptionalAnnotationUsage for example
            val nameResolver = nameResolver
            repeat(nameResolver.strings.stringCount) { stringIndex ->
                stringTable.addString(nameResolver.strings.getString(stringIndex))
            }
            repeat(nameResolver.qualifiedNames.qualifiedNameCount) { nameIndex ->
                val qualifiedName = nameResolver.qualifiedNames.getQualifiedName(nameIndex)
                stringTable.addQualifiedName(qualifiedName)
            }
            // Then add the annotations themselves, unless they are in dirty sources, i.e. contained in backendContext.optionalAnnotations
            for (proto in optionalAnnotations) {
                val name = nameResolver.getQualifiedClassName(proto.fqName)
                if (backendContext.optionalAnnotations.none { metadata -> metadata.name?.asString() == name }) {
                    builder.addOptionalAnnotationClass(proto)
                }
            }
        }

        for (part in backendContext.state.factory.packagePartRegistry.parts.values.addCompiledPartsAndSort(backendContext.state)) {
            part.addTo(builder)
        }

        for (metadata in backendContext.optionalAnnotations) {
            val serializer = backendContext.backendExtension.createModuleMetadataSerializer(backendContext)
            builder.addOptionalAnnotationClass(serializer.serializeOptionalAnnotationClass(metadata, stringTable))
        }

        val (stringTableProto, qualifiedNameTableProto) = stringTable.buildProto()
        builder.setStringTable(stringTableProto)
        builder.setQualifiedNameTable(qualifiedNameTableProto)

        backendContext.state.factory.setModuleMapping(builder.build())
    }
}
