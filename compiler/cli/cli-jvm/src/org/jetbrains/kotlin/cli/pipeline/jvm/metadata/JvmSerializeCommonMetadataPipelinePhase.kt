/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.pipeline.jvm.metadata

import org.jetbrains.kotlin.cli.pipeline.CheckCompilationErrors
import org.jetbrains.kotlin.cli.pipeline.PerformanceNotifications
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.jvm.JvmFrontendPipelineArtifact
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.config.icMetadataTracker
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.pipeline.AllModulesFrontendOutput
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.serialization.FirKLibSerializerExtension
import org.jetbrains.kotlin.fir.serialization.serializeSingleFirFile
import org.jetbrains.kotlin.util.metadataVersion
import java.io.File

internal object JvmSerializeCommonMetadataPipelinePhase : PipelinePhase<JvmFrontendPipelineArtifact, JvmFrontendPipelineArtifact>(
    name = "JvmSerializeCommonMetadataPipelinePhase",
    preActions = setOf(PerformanceNotifications.KlibWritingStarted),
    postActions = setOf(PerformanceNotifications.KlibWritingFinished, CheckCompilationErrors.CheckDiagnosticCollector)
) {
    override fun executePhase(input: JvmFrontendPipelineArtifact): JvmFrontendPipelineArtifact {
        serializeFragmentsIfNeeded(input)
        return input
    }

    private fun serializeFragmentsIfNeeded(input: JvmFrontendPipelineArtifact) {
        val commonFragmentOutputs = input.frontendOutput.outputs.dropLast(1)
        if (commonFragmentOutputs.isEmpty()) return

        val configuration = input.configuration
        val icMetadataTracker = configuration.icMetadataTracker ?: return
        val metadataVersion = configuration.metadataVersion()
        val languageVersionSettings = configuration.languageVersionSettings
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

                    icMetadataTracker.report(moduleName, sourceFile, packageFragment.toByteArray())
                }
            }
        }
    }
}

