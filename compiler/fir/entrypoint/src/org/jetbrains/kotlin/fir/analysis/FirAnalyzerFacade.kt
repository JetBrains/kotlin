/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.backend.jvm.serialization.JvmIdSignatureDescriptor
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.Fir2IrJvmSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import java.io.File

class FirAnalyzerFacade(
    val session: FirSession,
    val languageVersionSettings: LanguageVersionSettings,
    val ktFiles: Collection<KtFile> = emptyList(), // may be empty if light tree mode enabled
    val originalFiles: Collection<File> = emptyList(), // may be empty if light tree mode disabled
    val useLightTree: Boolean = false
) {
    private var firFiles: List<FirFile>? = null
    private var _scopeSession: ScopeSession? = null
    val scopeSession: ScopeSession
        get() = _scopeSession!!

    private var collectedDiagnostics: Map<FirFile, List<FirDiagnostic<*>>>? = null

    private fun buildRawFir() {
        if (firFiles != null) return
        val firProvider = (session.firProvider as FirProviderImpl)
        firFiles = if (useLightTree) {
            val builder = LightTree2Fir(session, firProvider.kotlinScopeProvider)
            originalFiles.map {
                builder.buildFirFile(it).also { firFile ->
                    firProvider.recordFile(firFile)
                }
            }
        } else {
            val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider)
            ktFiles.map {
                builder.buildFirFile(it).also { firFile ->
                    firProvider.recordFile(firFile)
                }
            }
        }
    }

    fun runResolution(): List<FirFile> {
        if (firFiles == null) buildRawFir()
        if (_scopeSession != null) return firFiles!!
        val resolveProcessor = FirTotalResolveProcessor(session)
        resolveProcessor.process(firFiles!!)
        _scopeSession = resolveProcessor.scopeSession
        return firFiles!!
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun runCheckers(): Map<FirFile, List<FirDiagnostic<*>>> {
        if (_scopeSession == null) runResolution()
        if (collectedDiagnostics != null) return collectedDiagnostics!!
        val collector = FirDiagnosticsCollector.create(session, scopeSession)
        collectedDiagnostics = buildMap {
            for (file in firFiles!!) {
                put(file, collector.collectDiagnostics(file))
            }
        }
        return collectedDiagnostics!!
    }

    fun convertToIr(extensions: GeneratorExtensions): Fir2IrResult {
        if (_scopeSession == null) runResolution()
        val signaturer = JvmIdSignatureDescriptor(JvmManglerDesc())

        return Fir2IrConverter.createModuleFragment(
            session, _scopeSession!!, firFiles!!,
            languageVersionSettings, signaturer,
            extensions, FirJvmKotlinMangler(session), IrFactoryImpl,
            FirJvmVisibilityConverter,
            Fir2IrJvmSpecialAnnotationSymbolProvider()
        )
    }
}
