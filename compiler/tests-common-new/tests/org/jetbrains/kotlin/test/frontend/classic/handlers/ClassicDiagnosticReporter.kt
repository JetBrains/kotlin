/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.classic.handlers

import org.jetbrains.kotlin.checkers.utils.DiagnosticsRenderingConfiguration
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.js.isJs
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.*

class ClassicDiagnosticReporter(private val testServices: TestServices) {
    private val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    fun createConfiguration(module: TestModule): DiagnosticsRenderingConfiguration {
        return DiagnosticsRenderingConfiguration(
            platform = null,
            languageVersionSettings = module.languageVersionSettings,
            skipDebugInfoDiagnostics = testServices.compilerConfigurationProvider.getCompilerConfiguration(module)
                .getBoolean(JVMConfigurationKeys.IR)
        )
    }

    fun reportDiagnostic(diagnostic: Diagnostic, module: TestModule, file: TestFile) {
        globalMetadataInfoHandler.addMetadataInfosForFile(file, diagnostic.toMetaInfo(module, file))
    }

    private fun Diagnostic.toMetaInfo(module: TestModule, file: TestFile): List<DiagnosticCodeMetaInfo> = textRanges.map { range ->
        val metaInfo = DiagnosticCodeMetaInfo(range, ClassicMetaInfoUtils.renderDiagnosticNoArgs, this)
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

