/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.renderConfigurations

import org.jetbrains.kotlin.checkers.diagnostics.factories.DebugInfoDiagnosticFactory1
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.model.DiagnosticCodeMetaInfo
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.rendering.*

open class DiagnosticCodeMetaInfoRenderConfiguration(
    val withNewInference: Boolean = true,
    val renderSeverity: Boolean = false
) : AbstractCodeMetaInfoRenderConfiguration() {
    private val crossPlatformLineBreak = """\r?\n""".toRegex()

    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo !is DiagnosticCodeMetaInfo) return ""
        return (getTag(codeMetaInfo)
                + getAttributesString(codeMetaInfo)
                + getParamsString(codeMetaInfo))
            .replace(crossPlatformLineBreak, "")
    }

    private fun getParamsString(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        if (!renderParams) return ""
        val params = mutableListOf<String>()

        @Suppress("UNCHECKED_CAST")
        val renderer = when (codeMetaInfo.diagnostic.factory) {
            is DebugInfoDiagnosticFactory1 -> DiagnosticWithParameters1Renderer(
                "{0}",
                Renderers.TO_STRING
            ) as DiagnosticRenderer<Diagnostic>
            else -> DefaultErrorMessages.getRendererForDiagnostic(codeMetaInfo.diagnostic)
        }
        if (renderer is AbstractDiagnosticWithParametersRenderer) {
            renderer.renderParameters(codeMetaInfo.diagnostic).mapTo(params) {
                it.toString().replace("\"", "\\\"")
            }
        }
        if (renderSeverity)
            params.add("severity='${codeMetaInfo.diagnostic.severity}'")

        params.add(getAdditionalParams(codeMetaInfo))

        return "(\"${params.filter { it.isNotEmpty() }.joinToString("; ")}\")"
    }

    fun getTag(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name
    }
}
