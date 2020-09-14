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

interface ICodeMetaInfo {

    val start: Int
    val end: Int
    val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration?

    fun asString(): String
}

class DiagnosticCodeMetaInfo(
    override val start: Int,
    override val end: Int,
    override val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration?,
    val diagnostic: Diagnostic
) :
    ICodeMetaInfo {

    override fun asString(): String {
        return renderConfiguration!!.asString(this)
    }
}

class LineMarkerCodeMetaInfo(
    override val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration?,
    val lineMarker: LineMarkerInfo<*>
) : ICodeMetaInfo {

    override val start: Int
        get() = lineMarker.startOffset
    override val end: Int
        get() = lineMarker.endOffset

    val withDescription = true

    override fun asString(): String {
        return renderConfiguration!!.asString(this)
    }
}

class HighlightingCodeMetaInfo(
    override val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration?,
    val highlightingInfo: HighlightInfo
) : ICodeMetaInfo {

    override val start: Int
        get() = highlightingInfo.startOffset
    override val end: Int
        get() = highlightingInfo.endOffset

    override fun asString(): String {
        return renderConfiguration!!.asString(this)
    }
}

object CodeMetaInfoFactory {
    private fun createCodeMetaInfo(obj: Any, renderConfiguration: AbstractCodeMetaInfoRenderConfiguration?): Collection<ICodeMetaInfo> {
        return when (obj) {
            is Diagnostic -> {
                if (renderConfiguration != null && renderConfiguration !is DiagnosticCodeMetaInfoRenderConfiguration)
                    throw IllegalArgumentException("Unexpected render configuration for CodeMetaInfo object $obj")
                obj.textRanges.map { DiagnosticCodeMetaInfo(it.start, it.end, renderConfiguration, obj) }
            }
            is ActualDiagnostic -> {
                if (renderConfiguration != null && renderConfiguration !is DiagnosticCodeMetaInfoRenderConfiguration)
                    throw IllegalArgumentException("Unexpected render configuration for CodeMetaInfo object $obj")
                obj.diagnostic.textRanges.map { DiagnosticCodeMetaInfo(it.start, it.end, renderConfiguration, obj.diagnostic) }
            }
            is HighlightInfo -> {
                if (renderConfiguration != null && renderConfiguration !is HighlightingRenderConfiguration)
                    throw IllegalArgumentException("Unexpected render configuration for CodeMetaInfo object $obj")
                listOf(HighlightingCodeMetaInfo(renderConfiguration, obj))
            }
            is LineMarkerInfo<*> -> {
                if (renderConfiguration != null && renderConfiguration !is LineMarkerRenderConfiguration)
                    throw IllegalArgumentException("Unexpected render configuration for CodeMetaInfo object $obj")
                listOf(LineMarkerCodeMetaInfo(renderConfiguration, obj))
            }
            else -> throw IllegalArgumentException("Unknown type for creating CodeMetaInfo object $obj")
        }
    }

    fun getCodeMetaInfo(
        objects: Collection<Any>,
        renderConfiguration: AbstractCodeMetaInfoRenderConfiguration?
    ): Collection<ICodeMetaInfo> {
        return objects.flatMap { createCodeMetaInfo(it, renderConfiguration) }
    }
}