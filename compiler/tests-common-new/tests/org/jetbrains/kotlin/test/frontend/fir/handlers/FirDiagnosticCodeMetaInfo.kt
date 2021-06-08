/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.fir.analysis.diagnostics.AbstractFirDiagnosticWithParametersRenderer
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDefaultErrorMessages
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic

object FirMetaInfoUtils {
    val renderDiagnosticNoArgs = FirDiagnosticCodeMetaRenderConfiguration().apply { renderParams = false }
    val renderDiagnosticWithArgs = FirDiagnosticCodeMetaRenderConfiguration().apply { renderParams = true }
}

class FirDiagnosticCodeMetaInfo(
    val diagnostic: FirDiagnostic<*>,
    renderConfiguration: FirDiagnosticCodeMetaRenderConfiguration
) : CodeMetaInfo {
    private val textRangeFromClassicDiagnostic: TextRange = run {
        diagnostic.factory.defaultPositioningStrategy.markDiagnostic(diagnostic).first()
    }

    override var renderConfiguration: FirDiagnosticCodeMetaRenderConfiguration = renderConfiguration
        private set

    override val start: Int
        get() = textRangeFromClassicDiagnostic.startOffset

    override val end: Int
        get() = textRangeFromClassicDiagnostic.endOffset

    override val tag: String
        get() = renderConfiguration.getTag(this)

    override val attributes: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    fun replaceRenderConfiguration(renderConfiguration: FirDiagnosticCodeMetaRenderConfiguration) {
        this.renderConfiguration = renderConfiguration
    }
}

class FirDiagnosticCodeMetaRenderConfiguration(
    val renderSeverity: Boolean = false,
) : AbstractCodeMetaInfoRenderConfiguration(renderParams = false) {
    private val crossPlatformLineBreak = """\r?\n""".toRegex()

    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo !is FirDiagnosticCodeMetaInfo) return ""
        return (getTag(codeMetaInfo)
                + getAttributesString(codeMetaInfo)
                + getParamsString(codeMetaInfo))
            .replace(crossPlatformLineBreak, "")
    }

    private fun getParamsString(codeMetaInfo: FirDiagnosticCodeMetaInfo): String {
        if (!renderParams) return ""
        val params = mutableListOf<String>()

        val diagnostic = codeMetaInfo.diagnostic

        @Suppress("UNCHECKED_CAST")
        val renderer = FirDefaultErrorMessages.getRendererForDiagnostic(diagnostic)
        if (renderer is AbstractFirDiagnosticWithParametersRenderer<*>) {
            renderer.renderParameters(diagnostic).mapTo(params, Any?::toString)
        }

        if (renderSeverity)
            params.add("severity='${diagnostic.severity}'")

        params.add(getAdditionalParams(codeMetaInfo))

        return "(\"${params.filter { it.isNotEmpty() }.joinToString("; ")}\")"
    }

    fun getTag(codeMetaInfo: FirDiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name
    }
}
