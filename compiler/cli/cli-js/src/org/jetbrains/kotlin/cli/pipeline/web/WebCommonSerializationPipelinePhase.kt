/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.web

import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.incremental.js.KmpIncrementalResultsConsumer
import org.jetbrains.kotlin.js.config.incrementalResultsConsumer
import org.jetbrains.kotlin.js.config.jsIncrementalCompilationEnabled
import org.jetbrains.kotlin.util.metadataVersion
import java.io.File

/**
 * Serializes metadata of common (non-leaf) fragment files as seen by their own fragment, i.e. keeping `expect` declarations.
 *
 * The klib metadata produced by [WebKlibSerializationPipelinePhase] is the platform view: actualized `expect` declarations
 * are dropped from it, so it can't be fed back to a common fragment session on the next incremental round.
 *
 * Adapted from the JVM counterpart, `org.jetbrains.kotlin.cli.pipeline.jvm.metadata.JvmSerializeCommonMetadataPipelinePhase`.
 */
internal object WebCommonSerializationPipelinePhase : PipelinePhase<WebFir2IrPipelineArtifact, WebFir2IrPipelineArtifact>(
    name = "WebCommonSerializationPipelinePhase",
) {
    override fun executePhase(input: WebFir2IrPipelineArtifact): WebFir2IrPipelineArtifact {
        serializeCommonMetadataIfNeeded(input)
        return input
    }

    // TODO (unify with JVM)
    private fun serializeCommonMetadataIfNeeded(input: WebFir2IrPipelineArtifact) {
        val configuration = input.configuration
        if (!configuration.jsIncrementalCompilationEnabled) return

        val commonFragmentOutputs = input.frontendOutput.outputs.dropLast(1)
        if (commonFragmentOutputs.isEmpty()) return

        val incrementalResultsConsumer = configuration.requireKmpIncrementalResultsConsumer()
        val languageVersionSettings = configuration.languageVersionSettings
        val metadataVersion = configuration.metadataVersion()
        val exportKDoc = languageVersionSettings.supportsFeature(LanguageFeature.ExportKDocDocumentationToKlib)

        commonFragmentOutputs.forEach { output ->
            val moduleName = output.session.moduleData.name.asStringStripSpecialMarkers()
            val firResult = AllModulesFrontendOutput(listOf(output))

            firResult.outputs.forEach { (session, scopeSession, fir) ->
                val serializerExtension = FirKLibSerializerExtension(
                    session = session,
                    scopeSession = scopeSession,
                    firProvider = session.firProvider,
                    metadataVersion = metadataVersion,
                    exportKDoc = exportKDoc,
                    additionalMetadataProvider = null,
                )

                fir.forEach { firFile ->
                    val sourceFile = firFile.sourceFile?.path?.let { File(it) }
                    if (sourceFile == null) {
                        // Fragments without a source path come from content generated for compiler plugins. We intentionally
                        // don't track them: IC always marks such generated files as dirty before compilation, so their
                        // metadata is regenerated on every round and never needs to be restored from the cache.
                        return@forEach
                    }

                    val packageFragment = serializeSingleFirFile(
                        firFile,
                        session,
                        scopeSession,
                        actualizedExpectDeclarations = null,
                        serializerExtension,
                        languageVersionSettings,
                    )

                    incrementalResultsConsumer.processCommonPackagePart(moduleName, sourceFile, packageFragment.toByteArray())
                }
            }
        }
    }

    private fun CompilerConfiguration.requireKmpIncrementalResultsConsumer(): KmpIncrementalResultsConsumer =
        requireNotNull(incrementalResultsConsumer as? KmpIncrementalResultsConsumer) {
            "Misconfiguration: `KmpIncrementalResultsConsumer` must be registered for KMP projects when incremental compilation is enabled."
        }
}
