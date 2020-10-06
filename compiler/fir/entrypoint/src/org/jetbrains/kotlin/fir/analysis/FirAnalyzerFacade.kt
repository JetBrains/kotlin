/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureDescriptor
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.collectors.FirDiagnosticsCollector
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.backend.Fir2IrConverter
import org.jetbrains.kotlin.fir.backend.Fir2IrResult
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmKotlinMangler
import org.jetbrains.kotlin.fir.backend.jvm.FirJvmVisibilityConverter
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveProcessor
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmManglerDesc
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.psi.KtFile

class FirAnalyzerFacade(val session: FirSession, val languageVersionSettings: LanguageVersionSettings, val ktFiles: List<KtFile>) {
    private var firFiles: List<FirFile>? = null
    private var scopeSession: ScopeSession? = null
    private var collectedDiagnostics: List<FirDiagnostic<*>>? = null

    private fun buildRawFir() {
        if (firFiles != null) return
        val firProvider = (session.firProvider as FirProviderImpl)
        val builder = RawFirBuilder(session, firProvider.kotlinScopeProvider, stubMode = false)
        firFiles = ktFiles.map {
            val firFile = builder.buildFirFile(it)
            firProvider.recordFile(firFile)
            firFile
        }
    }

    fun runResolution(): List<FirFile> {
        if (firFiles == null) buildRawFir()
        if (scopeSession != null) return firFiles!!
        val resolveProcessor = FirTotalResolveProcessor(session)
        resolveProcessor.process(firFiles!!)
        scopeSession = resolveProcessor.scopeSession
        return firFiles!!
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun runCheckers(): List<FirDiagnostic<*>> {
        if (scopeSession == null) runResolution()
        if (collectedDiagnostics != null) return collectedDiagnostics!!
        val collector = FirDiagnosticsCollector.create(session)
        collectedDiagnostics = buildList {
            for (file in firFiles!!) {
                addAll(collector.collectDiagnostics(file))
            }
        }
        return collectedDiagnostics!!
    }

    fun convertToIr(generateFacades: Boolean = true): Fir2IrResult {
        if (scopeSession == null) runResolution()
        val signaturer = IdSignatureDescriptor(JvmManglerDesc())

        return Fir2IrConverter.createModuleFragment(
            session, scopeSession!!, firFiles!!,
            languageVersionSettings, signaturer,
            JvmGeneratorExtensions(generateFacades), FirJvmKotlinMangler(session), IrFactoryImpl,
            FirJvmVisibilityConverter
        )
    }
}
