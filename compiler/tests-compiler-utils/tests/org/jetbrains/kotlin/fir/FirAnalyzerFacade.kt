/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.sourceFiles.LightTreeFile
import org.jetbrains.kotlin.test.FirParser

abstract class AbstractFirAnalyzerFacade {
    abstract val scopeSession: ScopeSession
    abstract fun runCheckers(): Map<FirFile, List<KtDiagnostic>>

    abstract fun runResolution(): List<FirFile>

    abstract fun convertToIr(
        fir2IrExtensions: Fir2IrExtensions,
        commonMemberStorage: Fir2IrCommonMemberStorage,
        irBuiltIns: IrBuiltInsOverFir?
    ): Fir2IrResult
}

class FirAnalyzerFacade(
    val session: FirSession,
    val fir2IrConfiguration: Fir2IrConfiguration,
    val ktFiles: Collection<KtFile> = emptyList(), // may be empty if light tree mode enabled
    val lightTreeFiles: Collection<LightTreeFile> = emptyList(), // may be empty if light tree mode disabled
    val irGeneratorExtensions: Collection<IrGenerationExtension>,
    val parser: FirParser,
    val enablePluginPhases: Boolean = false,
    val diagnosticReporterForLightTree: DiagnosticReporter? = null
) : AbstractFirAnalyzerFacade() {
    private var firFiles: List<FirFile>? = null
    private var _scopeSession: ScopeSession? = null
    override val scopeSession: ScopeSession
        get() = _scopeSession!!

    private var collectedDiagnostics: Map<FirFile, List<KtDiagnostic>>? = null

    private fun buildRawFir() {
        if (firFiles != null) return
        val firProvider = (session.firProvider as FirProviderImpl)
        firFiles = when (parser) {
            FirParser.LightTree -> {
                val builder = LightTree2Fir(session, firProvider.kotlinScopeProvider, diagnosticReporterForLightTree)
                lightTreeFiles.map {
                    builder.buildFirFile(it.lightTree, it.sourceFile, it.linesMapping).also { firFile ->
                        firProvider.recordFile(firFile)
                    }
                }
            }
            FirParser.Psi -> {
                val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider)
                ktFiles.map {
                    builder.buildFirFile(it).also { firFile ->
                        firProvider.recordFile(firFile)
                    }
                }
            }
        }
    }

    override fun runResolution(): List<FirFile> {
        if (firFiles == null) buildRawFir()
        if (_scopeSession != null) return firFiles!!
        val resolveProcessor = FirTotalResolveProcessor(session)
        resolveProcessor.process(firFiles!!)
        _scopeSession = resolveProcessor.scopeSession
        return firFiles!!
    }

    override fun runCheckers(): Map<FirFile, List<KtDiagnostic>> {
        if (_scopeSession == null) runResolution()
        if (collectedDiagnostics != null) return collectedDiagnostics!!
        val collector = FirDiagnosticsCollector.create(session, scopeSession)
        collectedDiagnostics = buildMap {
            for (file in firFiles!!) {
                withFileAnalysisExceptionWrapping(file) {
                    val reporter = DiagnosticReporterFactory.createPendingReporter()
                    collector.collectDiagnostics(file, reporter)
                    put(file, reporter.diagnostics)
                }
            }
        }
        return collectedDiagnostics!!
    }

    override fun convertToIr(
        fir2IrExtensions: Fir2IrExtensions,
        commonMemberStorage: Fir2IrCommonMemberStorage,
        irBuiltIns: IrBuiltInsOverFir?
    ): Fir2IrResult {
        if (_scopeSession == null) runResolution()

        return Fir2IrConverter.createModuleFragmentWithSignaturesIfNeeded(
            session, _scopeSession!!, firFiles!!,
            fir2IrExtensions,
            fir2IrConfiguration,
            JvmIrMangler, IrFactoryImpl,
            FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider(),
            irGeneratorExtensions,
            kotlinBuiltIns = DefaultBuiltIns.Instance, // TODO: consider passing externally,
            commonMemberStorage = commonMemberStorage,
            initializedIrBuiltIns = irBuiltIns
        )
    }
}
