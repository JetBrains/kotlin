/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.pipeline

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.ir.PreSerializationNativeSymbols.Impl
import org.jetbrains.kotlin.backend.common.phaser.PhaseEngine
import org.jetbrains.kotlin.backend.konan.KonanCompilationException
import org.jetbrains.kotlin.backend.konan.lower.SpecialBackendChecksTraversal
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.diagnosticsCollector
import org.jetbrains.kotlin.cli.hasMessageCollectorErrors
import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.phaseConfig
import org.jetbrains.kotlin.config.phaser.PhaseConfig
import org.jetbrains.kotlin.config.phaser.PhaserState
import org.jetbrains.kotlin.fir.backend.DelicateDeclarationStorageApi
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrExternalPackageFragment
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.isNativeStdlib
import org.jetbrains.kotlin.library.metadata.parseModuleHeader
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.native.Fir2IrOutput
import org.jetbrains.kotlin.native.NativeFir2IrExtensions
import org.jetbrains.kotlin.native.runPreSerializationLowerings

object NativeFir2IrPipelinePhase : PipelinePhase<NativeFrontendArtifact, NativeFir2IrArtifact>(
    name = "NativeFir2IrPhase",
    preActions = setOf(PerformanceNotifications.TranslationToIrStarted),
    postActions = setOf(PerformanceNotifications.TranslationToIrFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFrontendArtifact): NativeFir2IrArtifact? {
        val (frontendOutput, configuration, phaseContext) = input
        val loadedKlibs = phaseContext.config.loadedKlibs
        val diagnosticsReporter = configuration.diagnosticsCollector
        val fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(configuration, diagnosticsReporter)
        val actualizedResult = frontendOutput.convertToIrAndActualize(
            NativeFir2IrExtensions,
            fir2IrConfiguration,
            configuration.getCompilerExtensions(IrGenerationExtension),
            irMangler = KonanManglerIr,
            visibilityConverter = Fir2IrVisibilityConverter.Default,
            kotlinBuiltIns = DefaultBuiltIns.Instance,
            specialAnnotationsProvider = null,
            extraActualDeclarationExtractorsInitializer = { emptyList() },
            typeSystemContextProvider = ::IrTypeSystemContextImpl,
        )

        @OptIn(DelicateDeclarationStorageApi::class)
        val usedPackages = buildSet {
            val queue = ArrayDeque<IrSymbol>()
            val processed = mutableSetOf<IrSymbol>()

            fun queueSymbol(symbol: IrSymbol) {
                if (processed.add(symbol)) {
                    queue.add(symbol)
                }
            }

            actualizedResult.components.declarationStorage.forEachCachedDeclarationSymbol(::queueSymbol)
            actualizedResult.components.classifierStorage.forEachCachedDeclarationSymbol(::queueSymbol)

            while (queue.isNotEmpty()) {
                val symbol = queue.removeFirst()

                // FIXME(KT-64742): Fir2IrDeclarationStorage caches may contain unbound IR symbols, so we filter them out.
                val p = symbol.takeIf { it.isBound }?.owner as? IrDeclaration ?: continue
                val fragment = (p.getPackageFragment() as? IrExternalPackageFragment) ?: continue
                if (symbol is IrClassifierSymbol) {
                    symbol.superTypes().forEach { it.classOrNull?.let(::queueSymbol) }
                }
                add(fragment.packageFqName)
            }

            // These packages exist in all platform libraries, but can contain only synthetic declarations.
            // These declarations are not really located in klib, so we don't need to depend on klib to use them.
            removeAll(NativeForwardDeclarationKind.entries.map { it.packageFqName }.toSet())
        }.toList()
        val usedLibraries = loadedKlibs.all.filter { library ->
            val header = parseModuleHeader(library.metadata.moduleHeaderData)

            val nonEmptyPackageNames = buildSet {
                addAll(header.packageFragmentNameList)
                removeAll(header.emptyPackageList)
            }

            usedPackages.any { it.asString() in nonEmptyPackageNames }
        }.toSet()
        loadedKlibs.all.find { it.isNativeStdlib }?.let {
            require(usedLibraries.contains(it)) {
                "Internal error: stdlib must be in usedLibraries, if it's in resolvedLibraries"
            }
        }
        val symbols = Impl(actualizedResult.irBuiltIns)
        if (diagnosticsReporter.hasErrors) {
            throw CompilationErrorException("Compilation failed: there were some diagnostics during fir2ir")
        }
        val fir2IrResult = Fir2IrOutput(frontendOutput, symbols, actualizedResult, usedLibraries)
        try {
            SpecialBackendChecksTraversal(
                phaseContext,
                fir2IrResult.symbols,
                fir2IrResult.fir2irActualizedResult.irBuiltIns,
            ).lower(fir2IrResult.fir2irActualizedResult.irModuleFragment)
        } catch (_: KonanCompilationException) {
            require(configuration.hasMessageCollectorErrors())
            return null
        }

        return NativeFir2IrArtifact(
            fir2IrOutput = fir2IrResult,
            configuration = configuration,
            phaseContext = phaseContext,
        )
    }
}

object NativePreSerializationPipelinePhase : PipelinePhase<NativeFir2IrArtifact, NativeFir2IrArtifact>(
    name = "NativePreSerializationPhase",
    preActions = setOf(PerformanceNotifications.IrPreLoweringStarted),
    postActions = setOf(PerformanceNotifications.IrPreLoweringFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: NativeFir2IrArtifact): NativeFir2IrArtifact {
        val (fir2IrOutput, configuration, phaseContext) = input
        val phaseConfig = configuration.phaseConfig ?: PhaseConfig()
        val phaserState = PhaserState()
        val engine = PhaseEngine(phaseConfig, phaserState, phaseContext)
        val loweredResult = engine.runPreSerializationLowerings(fir2IrOutput, configuration)
        return NativeFir2IrArtifact(
            fir2IrOutput = loweredResult,
            configuration = configuration,
            phaseContext = phaseContext,
        )
    }
}
