/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibSingleFileMetadataSerializer
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.ConstValueProviderImpl
import org.jetbrains.kotlin.fir.backend.extractFirDeclarations
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.FirProvider
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.library.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName

/**
 * Responsible for serializing a FIR file metadata into a protobuf to be later written to a KLIB.
 */
class Fir2KlibMetadataSerializer(
    compilerConfiguration: CompilerConfiguration,
    private val firOutputs: List<ModuleCompilerAnalyzedOutput>,
    private val fir2IrActualizedResult: Fir2IrActualizedResult?,
    private val exportKDoc: Boolean,
    private val produceHeaderKlib: Boolean,
) : KlibSingleFileMetadataSerializer<FirFile> {

    private val firFilesAndSessions: Map<FirFile, Pair<FirSession, ScopeSession>> =
        buildMap {
            for (firOutput in firOutputs) {
                for (firFile in firOutput.fir) {
                    put(firFile, firOutput.session to firOutput.scopeSession)
                }
            }
        }

    private val actualizedExpectDeclarations by lazy {
        fir2IrActualizedResult?.irActualizedResult?.actualizedExpectDeclarations?.extractFirDeclarations()
    }

    private val languageVersionSettings = compilerConfiguration.languageVersionSettings

    private val metadataVersion = compilerConfiguration.get(CommonConfigurationKeys.METADATA_VERSION) as? KlibMetadataVersion
        ?: KlibMetadataVersion.INSTANCE

    /**
     * The list of source files whose metadata is to be serialized.
     */
    val sourceFiles: List<KtSourceFile> = firFilesAndSessions.keys.map { it.sourceFile!! }

    override val numberOfSourceFiles: Int
        get() = firFilesAndSessions.size

    override fun serializeSingleFileMetadata(file: FirFile): ProtoBuf.PackageFragment {
        val session: FirSession
        val scopeSession: ScopeSession
        val firProvider: FirProvider
        val components = fir2IrActualizedResult?.components
        if (components != null) {
            session = components.session
            scopeSession = components.scopeSession
            firProvider = components.firProvider
        } else {
            val sessionAndScopeSession = firFilesAndSessions[file] ?: error("Missing FirSession and ScopeSession")
            session = sessionAndScopeSession.first
            scopeSession = sessionAndScopeSession.second
            firProvider = session.firProvider
        }
        return serializeSingleFirFile(
            file,
            session,
            scopeSession,
            actualizedExpectDeclarations,
            FirKLibSerializerExtension(
                session,
                firProvider,
                metadataVersion,
                components?.let(::ConstValueProviderImpl),
                allowErrorTypes = false,
                exportKDoc,
                components?.annotationsFromPluginRegistrar?.createAdditionalMetadataProvider(),
            ),
            languageVersionSettings,
            produceHeaderKlib,
        )
    }

    override fun forEachFile(block: (Int, FirFile, KtSourceFile, FqName) -> Unit) {
        firFilesAndSessions.keys.forEachIndexed { i, firFile ->
            block(i, firFile, firFile.sourceFile!!, firFile.packageFqName)
        }
    }
}