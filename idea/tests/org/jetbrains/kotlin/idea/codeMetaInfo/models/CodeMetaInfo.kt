/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo.models

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import org.jetbrains.kotlin.checkers.diagnostics.ActualDiagnostic
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.HighlightingRenderConfiguration
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.LineMarkerRenderConfiguration
import org.jetbrains.kotlin.idea.editor.fixers.end
import org.jetbrains.kotlin.idea.editor.fixers.start

interface CodeMetaInfo {
    val start: Int
    val end: Int
    val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration
    val platforms: MutableList<String>

    fun asString(): String
    fun getTag(): String
}

class DiagnosticCodeMetaInfo(
    override val start: Int,
    override val end: Int,
    override val renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration,
    val diagnostic: Diagnostic
) : CodeMetaInfo {
    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    override fun getTag(): String = renderConfiguration.getTag(this)
}

class LineMarkerCodeMetaInfo(
    override val renderConfiguration: LineMarkerRenderConfiguration,
    val lineMarker: LineMarkerInfo<*>
) : CodeMetaInfo {
    override val start: Int
        get() = lineMarker.startOffset
    override val end: Int
        get() = lineMarker.endOffset
    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    override fun getTag(): String = renderConfiguration.getTag()
}

class HighlightingCodeMetaInfo(
    override val renderConfiguration: HighlightingRenderConfiguration,
    val highlightingInfo: HighlightInfo
) : CodeMetaInfo {
    override val start: Int
        get() = highlightingInfo.startOffset
    override val end: Int
        get() = highlightingInfo.endOffset
    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)

    override fun getTag(): String = renderConfiguration.getTag()
}

class ParsedCodeMetaInfo(
    override val start: Int,
    override val end: Int,
    override val platforms: MutableList<String>,
    private val tag: String
) : CodeMetaInfo {
    override val renderConfiguration = object : AbstractCodeMetaInfoRenderConfiguration(false) {}

    override fun asString(): String = renderConfiguration.asString(this)

    override fun getTag(): String = tag

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is CodeMetaInfo) return false
        return this.tag == other.getTag() && this.start == other.start && this.end == other.end
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        result = 31 * result + tag.hashCode()
        return result
    }
}

fun createCodeMetaInfo(obj: Any, renderConfiguration: AbstractCodeMetaInfoRenderConfiguration): List<CodeMetaInfo> {
    fun errorMessage() = "Unexpected render configuration for object $obj"
    return when (obj) {
        is Diagnostic -> {
            require(renderConfiguration is DiagnosticCodeMetaInfoRenderConfiguration, ::errorMessage)
            obj.textRanges.map { DiagnosticCodeMetaInfo(it.start, it.end, renderConfiguration, obj) }
        }
        is ActualDiagnostic -> {
            require(renderConfiguration is DiagnosticCodeMetaInfoRenderConfiguration, ::errorMessage)
            obj.diagnostic.textRanges.map { DiagnosticCodeMetaInfo(it.start, it.end, renderConfiguration, obj.diagnostic) }
        }
        is HighlightInfo -> {
            require(renderConfiguration is HighlightingRenderConfiguration, ::errorMessage)
            listOf(HighlightingCodeMetaInfo(renderConfiguration, obj))
        }
        is LineMarkerInfo<*> -> {
            require(renderConfiguration is LineMarkerRenderConfiguration, ::errorMessage)
            listOf(LineMarkerCodeMetaInfo(renderConfiguration, obj))
        }
        else -> throw IllegalArgumentException("Unknown type for creating CodeMetaInfo object $obj")
    }
}

fun getCodeMetaInfo(
    objects: List<Any>,
    renderConfiguration: AbstractCodeMetaInfoRenderConfiguration
): List<CodeMetaInfo> {
    return objects.flatMap { createCodeMetaInfo(it, renderConfiguration) }
}
