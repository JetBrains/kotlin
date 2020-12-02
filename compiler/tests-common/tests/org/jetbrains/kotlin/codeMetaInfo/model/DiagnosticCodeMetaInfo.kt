/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.model

import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration
import org.jetbrains.kotlin.diagnostics.Diagnostic

class DiagnosticCodeMetaInfo(
    override val start: Int,
    override val end: Int,
    override val renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration,
    val diagnostic: Diagnostic
) : CodeMetaInfo {
    constructor(
        range: TextRange,
        renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration,
        diagnostic: Diagnostic
    ) : this(range.startOffset, range.endOffset, renderConfiguration, diagnostic)

    override val tag: String
        get() = renderConfiguration.getTag(this)

    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)
}
