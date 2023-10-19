/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.frontend.fir.handlers

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.diagnostics.AbstractKtDiagnosticWithParametersRenderer
import org.jetbrains.kotlin.diagnostics.KtDiagnostic
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

object FirMetaInfoUtils {
    val renderDiagnosticNoArgs = FirDiagnosticCodeMetaRenderConfiguration().apply { renderParams = false }
    val renderDiagnosticWithArgs = FirDiagnosticCodeMetaRenderConfiguration().apply { renderParams = true }
}

class FirDiagnosticCodeMetaInfo(
    val diagnostic: KtDiagnostic,
    renderConfiguration: FirDiagnosticCodeMetaRenderConfiguration,
    private val range: TextRange
) : CodeMetaInfo {
    override var renderConfiguration: FirDiagnosticCodeMetaRenderConfiguration = renderConfiguration
        private set

    override val start: Int
        get() = range.startOffset

    override val end: Int
        get() = range.endOffset

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

        val renderer = RootDiagnosticRendererFactory(diagnostic)
        if (renderer is AbstractKtDiagnosticWithParametersRenderer) {
            renderer.renderParameters(diagnostic).mapTo(params) {
                it.toString().replace("\"", "\\\"")
            }
        }

        if (renderSeverity)
            params.add("severity='${diagnostic.severity}'")

        params.add(getAdditionalParams(codeMetaInfo))
        val nonEmptyParams = params.filter { it.isNotEmpty() }

        return if (nonEmptyParams.isNotEmpty()) {
            "(\"${params.filter { it.isNotEmpty() }.joinToString("; ")}\")"
        } else {
            ""
        }
    }

    fun getTag(codeMetaInfo: FirDiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name
    }
}
