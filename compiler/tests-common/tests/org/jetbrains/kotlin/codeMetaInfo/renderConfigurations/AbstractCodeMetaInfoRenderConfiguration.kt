/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.renderConfigurations

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo

abstract class AbstractCodeMetaInfoRenderConfiguration(var renderParams: Boolean = true) {
    private val clickOrPressRegex = "Click or press (.*)to navigate".toRegex() // We have different hotkeys on different platforms
    open fun asString(codeMetaInfo: CodeMetaInfo): String = codeMetaInfo.tag + getPlatformsString(codeMetaInfo)

    open fun getAdditionalParams(codeMetaInfo: CodeMetaInfo) = ""

    protected fun sanitizeLineMarkerTooltip(originalText: String?): String {
        if (originalText == null) return "null"
        val noHtmlTags = StringUtil.removeHtmlTags(originalText)
            .replace(" ", "")
            .replace(clickOrPressRegex, "")
            .trim()
        return sanitizeLineBreaks(noHtmlTags)
    }

    protected fun sanitizeLineBreaks(originalText: String): String {
        var sanitizedText = originalText
        sanitizedText = StringUtil.replace(sanitizedText, "\r\n", " ")
        sanitizedText = StringUtil.replace(sanitizedText, "\n", " ")
        sanitizedText = StringUtil.replace(sanitizedText, "\r", " ")
        return sanitizedText
    }

    protected fun getPlatformsString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo.platforms.isEmpty()) return ""
        return "{${codeMetaInfo.platforms.joinToString(";")}}"
    }
}
