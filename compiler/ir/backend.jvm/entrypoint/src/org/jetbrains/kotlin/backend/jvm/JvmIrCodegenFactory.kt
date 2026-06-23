/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.isBytecodeGenerationSuppressed
import org.jetbrains.kotlin.backend.common.ir.isJvmBuiltin
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.EnumEntriesIntrinsicMappingsCacheImpl
import org.jetbrains.kotlin.backend.jvm.codegen.JvmIrIntrinsicExtension
import org.jetbrains.kotlin.backend.jvm.intrinsics.IrIntrinsicMethods
import org.jetbrains.kotlin.backend.jvm.ir.getIoFile
import org.jetbrains.kotlin.backend.jvm.ir.getKtFile
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAMES
import org.jetbrains.kotlin.codegen.addCompiledPartsAndSort
import org.jetbrains.kotlin.codegen.loadCompiledModule
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.ExternalDependenciesGenerator
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.metadata.jvm.JvmModuleProtoBuf
import org.jetbrains.kotlin.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.SerializableStringTable
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.UnitStats
import org.jetbrains.kotlin.util.tryMeasurePhaseTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JvmIrCodegenFactory(
    private val configuration: CompilerConfiguration,
    private val ideCodegenSettings: IdeCodegenSettings = IdeCodegenSettings(),
) {
    /**
     * @param shouldStubAndNotLinkUnboundSymbols
     * must be `true` only if current compilation is done in the context of the "Evaluate Expression"
     * process in the debugger or "Android LiveEdit plugin".
     * When enabled, this option disables the linkage process and generates stubs for all unbound symbols.
     * @param shouldStubOrphanedExpectSymbols is K1 only and should be removed
     */
    data class IdeCodegenSettings(
        val shouldStubAndNotLinkUnboundSymbols: Boolean = false,
        @property:K1Deprecation
        val shouldStubOrphanedExpectSymbols: Boolean = false,
        val evaluatorData: JvmEvaluatorData? = null,
    )

    data class BackendInput(
        val irModuleFragment: IrModuleFragment,
        val irBuiltIns: IrBuiltIns,
        val symbolTable: SymbolTable,
        val irProviders: List<IrProvider>,
        val debuggerExtensions: JvmDebuggerExtensions?,
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

    private val CompilerConfiguration.filteredExtensions: List<IrGenerationExtension>
        get() = this.getCompilerExtensions(IrGenerationExtension)


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
        (val irModuleFragment, val irBuiltIns, val symbolTable, val irProviders, val debuggerExtensions, val backendExtension, val irPluginContext = pluginContext) = input

        val evaluatorData = ideCodegenSettings.evaluatorData
        val context = JvmBackendContext(
            state, irBuiltIns, symbolTable, debuggerExtensions,
            backendExtension, irPluginContext, evaluatorData
        )
        val generationExtensions = state.configuration.filteredExtensions
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
        val executor = if (nThreads > 1 && module.files.size > 1) Executors.newFixedThreadPool(nThreads) else null

        val perfManager = configuration.perfManager

        // Generate multifile facades first, to compute and store JVM signatures of const properties which are later used
        // when serializing metadata in the multifile parts.
        // TODO: consider dividing codegen itself into separate phases (bytecode generation, metadata serialization) to avoid this
        for (generateMultifileFacades in listOf(true, false)) {
            if (executor != null) {
                val tasks = mutableListOf<CompletableFuture<Void>>()
                val childrenStats = mutableListOf<UnitStats>()

                for (irFile in module.files) {
                    tasks.add(
                        CompletableFuture.runAsync(
                            {
                                val childPerfManager = PerformanceManagerImpl.createChildIfNeeded(perfManager, start = false)
                                childPerfManager.tryMeasurePhaseTime(PhaseType.Backend) {
                                    generateFile(context, irFile, intrinsicExtensions, generateMultifileFacades)
                                }
                                childPerfManager?.let { childrenStats.add(it.unitStats) }
                            },
                            executor
                        )
                    )
                }
                CompletableFuture.allOf(*tasks.toTypedArray()).get()

                if (perfManager != null) {
                    childrenStats.forEach { perfManager.addOtherUnitStats(it) }
                }
            } else {
                perfManager.tryMeasurePhaseTime(PhaseType.Backend) {
                    for (irFile in module.files) {
                        generateFile(context, irFile, intrinsicExtensions, generateMultifileFacades)
                    }
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
        for ([packageName, serialized] in serializedPackages) {
            context.state.factory.addSerializedBuiltinsPackageMetadata(
                BuiltInSerializerProtocol.getBuiltInsFilePath(packageName),
                serialized
            )
        }
    }

    private fun generateModuleMetadata(backendContext: JvmBackendContext) {
        val builder = JvmModuleProtoBuf.Module.newBuilder()
        val stringTable = SerializableStringTable()

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

        val [stringTableProto, qualifiedNameTableProto] = stringTable.buildProto()
        builder.setStringTable(stringTableProto)
        builder.setQualifiedNameTable(qualifiedNameTableProto)

        backendContext.state.factory.setModuleMapping(builder.build())
    }
}
