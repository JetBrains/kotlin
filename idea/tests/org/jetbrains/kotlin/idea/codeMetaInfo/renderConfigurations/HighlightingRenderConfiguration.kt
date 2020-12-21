/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations

import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.idea.codeMetaInfo.models.HighlightingCodeMetaInfo

open class HighlightingRenderConfiguration(
    val renderDescription: Boolean = true,
    val renderTextAttributesKey: Boolean = true,
    val renderSeverity: Boolean = true
) : AbstractCodeMetaInfoRenderConfiguration() {

    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo !is HighlightingCodeMetaInfo) return ""
        return getTag() + getParamsString(codeMetaInfo)
    }

    fun getTag() = "HIGHLIGHTING"

    private fun getParamsString(highlightingCodeMetaInfo: HighlightingCodeMetaInfo): String {
        if (!renderParams) return ""

        val params = mutableListOf<String>()
        if (renderSeverity)
            params.add("severity='${highlightingCodeMetaInfo.highlightingInfo.severity}'")
        if (renderDescription)
            params.add("descr='${sanitizeLineBreaks(highlightingCodeMetaInfo.highlightingInfo.description)}'")
        if (renderTextAttributesKey)
            params.add("textAttributesKey='${highlightingCodeMetaInfo.highlightingInfo.forcedTextAttributesKey}'")

        params.add(getAdditionalParams(highlightingCodeMetaInfo))
        val paramsString = params.filter { it.isNotEmpty() }.joinToString("; ")

        return if (paramsString.isEmpty()) "" else "(\"$paramsString\")"
    }
}
