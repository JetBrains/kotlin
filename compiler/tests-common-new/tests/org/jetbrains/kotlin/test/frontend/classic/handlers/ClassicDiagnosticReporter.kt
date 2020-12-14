/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.ParsedCodeMetaInfo
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.directives.DiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class ClassicDiagnosticReporter(private val testServices: TestServices) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    fun createConfiguration(module: TestModule): DiagnosticsRenderingConfiguration {
        return DiagnosticsRenderingConfiguration(
            platform = null,
            withNewInference = module.languageVersionSettings.supportsFeature(LanguageFeature.NewInference),
            languageVersionSettings = module.languageVersionSettings,
            skipDebugInfoDiagnostics = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                .getBoolean(JVMConfigurationKeys.IR)
        )
    }

    fun reportDiagnostic(
        diagnostic: Diagnostic,
        module: TestModule,
        file: TestFile,
        configuration: DiagnosticsRenderingConfiguration,
        withNewInferenceModeEnabled: Boolean
    ) {
        globalMetadataInfoHandler.addMetadataInfosForFile(
            file,
            diagnostic.toMetaInfo(
                module,
                file,
                configuration.withNewInference,
                withNewInferenceModeEnabled
            )
        )
    }

    private fun Diagnostic.toMetaInfo(
        module: TestModule,
        file: TestFile,
        newInferenceEnabled: Boolean,
        withNewInferenceModeEnabled: Boolean
    ): List<DiagnosticCodeMetaInfo> = textRanges.map { range ->
        val metaInfo = DiagnosticCodeMetaInfo(range, ClassicMetaInfoUtils.renderDiagnosticNoArgs, this)
        if (withNewInferenceModeEnabled) {
            metaInfo.attributes += if (newInferenceEnabled) OldNewInferenceMetaInfoProcessor.NI else OldNewInferenceMetaInfoProcessor.OI
        }
        if (file !in module.files) {
            val targetPlatform = module.targetPlatform
            metaInfo.attributes += when {
                targetPlatform.isJvm() -> "JVM"
                targetPlatform.isJs() -> "JS"
                targetPlatform.isNative() -> "NATIVE"
                targetPlatform.isCommon() -> "COMMON"
                else -> error("Should not be here")
            }
        }
        val existing = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, metaInfo)
        if (existing.any { it.description != null }) {
            metaInfo.replaceRenderConfiguration(ClassicMetaInfoUtils.renderDiagnosticWithArgs)
        }
        metaInfo
    }
}

class OldNewInferenceMetaInfoProcessor(testServices: TestServices) : AdditionalMetaInfoProcessor(testServices) {
    companion object {
        const val OI = "OI"
        const val NI = "NI"
    }

    override fun processMetaInfos(module: TestModule, file: TestFile) {
        /*
         * Rules for OI/NI attribute:
         * ┌──────────┬──────┬──────┬──────────┐
         * │          │  OI  │  NI  │ nothing  │ <- reported
         * ├──────────┼──────┼──────┼──────────┤
         * │  nothing │ both │ both │ nothing  │
         * │    OI    │  OI  │ both │   OI     │
         * │    NI    │ both │  NI  │   NI     │
         * │   both   │ both │ both │ opposite │ <- OI if NI enabled in test and vice versa
         * └──────────┴──────┴──────┴──────────┘
         *       ^ existed
         */
        if (!testServices.withNewInferenceModeEnabled()) return
        val newInferenceEnabled = module.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        val (currentFlag, otherFlag) = when (newInferenceEnabled) {
            true -> NI to OI
            false -> OI to NI
        }
        val matchedExistedInfos = mutableSetOf<ParsedCodeMetaInfo>()
        val matchedReportedInfos = mutableSetOf<CodeMetaInfo>()
        val allReportedInfos = globalMetadataInfoHandler.getReportedMetaInfosForFile(file)
        for ((_, reportedInfos) in allReportedInfos.groupBy { Triple(it.start, it.end, it.tag) }) {
            val existedInfos = globalMetadataInfoHandler.getExistingMetaInfosForActualMetadata(file, reportedInfos.first())
            for ((reportedInfo, existedInfo) in reportedInfos.zip(existedInfos)) {
                matchedExistedInfos += existedInfo
                matchedReportedInfos += reportedInfo
                if (currentFlag !in reportedInfo.attributes) continue
                if (currentFlag in existedInfo.attributes) continue
                reportedInfo.attributes.remove(currentFlag)
            }
        }

        if (allReportedInfos.size != matchedReportedInfos.size) {
            for (info in allReportedInfos) {
                if (info !in matchedReportedInfos) {
                    info.attributes.remove(currentFlag)
                }
            }
        }

        val allExistedInfos = globalMetadataInfoHandler.getExistingMetaInfosForFile(file)
        if (allExistedInfos.size == matchedExistedInfos.size) return

        val newInfos = allExistedInfos.mapNotNull {
            if (it in matchedExistedInfos) return@mapNotNull null
            if (currentFlag in it.attributes) return@mapNotNull null
            it.copy().apply {
                if (otherFlag !in attributes) {
                    attributes += otherFlag
                }
            }
        }
        globalMetadataInfoHandler.addMetadataInfosForFile(file, newInfos)
    }
}


fun TestServices.withNewInferenceModeEnabled(): Boolean {
    return DiagnosticsDirectives.WITH_NEW_INFERENCE in moduleStructure.allDirectives
}
