/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native

import org.jetbrains.kotlin.analyzer.CompilationErrorException
import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.backend.konan.ir.BackendNativeSymbols
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.fir.backend.DelicateDeclarationStorageApi
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.common.fir.FirDiagnosticsCompilerResultsReporter
import org.jetbrains.kotlin.cli.common.renderDiagnosticInternalName
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.fir.backend.Fir2IrConfiguration
import org.jetbrains.kotlin.fir.backend.Fir2IrVisibilityConverter
import org.jetbrains.kotlin.fir.pipeline.convertToIrAndActualize
import org.jetbrains.kotlin.ir.IrBuiltIns
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

fun PhaseContext.fir2Ir(
        input: FirOutput.Full,
): Fir2IrOutput {
    val resolvedLibraries = config.resolvedLibraries.getFullResolvedList()
    val configuration = config.configuration
    val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
    val diagnosticsReporter = DiagnosticReporterFactory.createPendingReporter()

    val fir2IrConfiguration = Fir2IrConfiguration.forKlibCompilation(configuration, diagnosticsReporter)
    val actualizedResult = input.firResult.convertToIrAndActualize(
        NativeFir2IrExtensions,
        fir2IrConfiguration,
        config.configuration.getCompilerExtensions(IrGenerationExtension),
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


    val usedLibraries = resolvedLibraries.filter { resolvedLibrary ->
        val header = parseModuleHeader(resolvedLibrary.library.metadata.moduleHeaderData)

        val nonEmptyPackageNames = buildSet {
            addAll(header.packageFragmentNameList)
            removeAll(header.emptyPackageList)
        }

        usedPackages.any { it.asString() in nonEmptyPackageNames }
    }.toSet()

    resolvedLibraries.find { it.library.isNativeStdlib }?.let {
        require(usedLibraries.contains(it)) {
            "Internal error: stdlib must be in usedLibraries, if it's in resolvedLibraries"
        }
    }

    val symbols = createKonanSymbols(actualizedResult.irBuiltIns)

    val renderDiagnosticNames = configuration.renderDiagnosticInternalName
    FirDiagnosticsCompilerResultsReporter.reportToMessageCollector(diagnosticsReporter, messageCollector, renderDiagnosticNames)

    if (diagnosticsReporter.hasErrors) {
        throw CompilationErrorException("Compilation failed: there were some diagnostics during fir2ir")
    }

    return Fir2IrOutput(input.firResult, symbols, actualizedResult, usedLibraries)
}

private fun PhaseContext.createKonanSymbols(
        irBuiltIns: IrBuiltIns,
): BackendNativeSymbols {
    return BackendNativeSymbols(this, irBuiltIns, this.config.configuration)
}
