/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporterFactory
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.builder.PsiHandlingMode
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import java.io.File

abstract class AbstractFirAnalyzerFacade {
    abstract val scopeSession: ScopeSession
    abstract fun runCheckers(): Map<FirFile, List<KtDiagnostic>>

    abstract fun runResolution(): List<FirFile>

    abstract fun convertToIr(extensions: GeneratorExtensions): Fir2IrResult
}

class FirAnalyzerFacade(
    val session: FirSession,
    val languageVersionSettings: LanguageVersionSettings,
    val ktFiles: Collection<KtFile> = emptyList(), // may be empty if light tree mode enabled
    val originalFiles: Collection<File> = emptyList(), // may be empty if light tree mode disabled
    val irGeneratorExtensions: Collection<IrGenerationExtension>,
    val useLightTree: Boolean = false,
    val enablePluginPhases: Boolean = false,
) : AbstractFirAnalyzerFacade() {
    private var firFiles: List<FirFile>? = null
    private var _scopeSession: ScopeSession? = null
    override val scopeSession: ScopeSession
        get() = _scopeSession!!

    private var collectedDiagnostics: Map<FirFile, List<KtDiagnostic>>? = null

    private fun buildRawFir() {
        if (firFiles != null) return
        val firProvider = (session.firProvider as FirProviderImpl)
        firFiles = if (useLightTree) {
            // In 1.6.1x IDE plugin fir compiler is packed right now, but :compiler:fir:raw-fir:light-tree2fir is not, because of that
            // plugin verification error is reported
            error("light tree mode is not supported")
        } else {
            val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider, PsiHandlingMode.COMPILER)
            ktFiles.map {
                builder.buildFirFile(it).also { firFile ->
                    firProvider.recordFile(firFile)
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

    @OptIn(ExperimentalStdlibApi::class)
    override fun runCheckers(): Map<FirFile, List<KtDiagnostic>> {
        if (_scopeSession == null) runResolution()
        if (collectedDiagnostics != null) return collectedDiagnostics!!
        val collector = FirDiagnosticsCollector.create(session, scopeSession)
        collectedDiagnostics = buildMap {
            for (file in firFiles!!) {
                val reporter = DiagnosticReporterFactory.createReporter()
                collector.collectDiagnostics(file, reporter)
                put(file, reporter.diagnostics)
            }
        }
        return collectedDiagnostics!!
    }

    override fun convertToIr(extensions: GeneratorExtensions): Fir2IrResult {
        if (_scopeSession == null) runResolution()
        val signaturer = JvmIdSignatureDescriptor(JvmDescriptorMangler(null))

        val commonFirFiles = session.moduleData.dependsOnDependencies
            .map { it.session }
            .filter { it.kind == FirSession.Kind.Source }
            .flatMap { (it.firProvider as FirProviderImpl).getAllFirFiles() }

        return Fir2IrConverter.createModuleFragment(
            session, _scopeSession!!, firFiles!! + commonFirFiles,
            languageVersionSettings, signaturer,
            extensions, FirJvmKotlinMangler(session), IrFactoryImpl,
            FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider(),
            irGeneratorExtensions
        )
    }
}
