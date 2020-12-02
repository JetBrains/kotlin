/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo.models

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.HighlightingRenderConfiguration

class HighlightingCodeMetaInfo(
    override val renderConfiguration: HighlightingRenderConfiguration,
    val highlightingInfo: HighlightInfo
) : CodeMetaInfo {
    override val start: Int
        get() = highlightingInfo.startOffset
    override val end: Int
        get() = highlightingInfo.endOffset

    override val tag: String
        get() = renderConfiguration.getTag()

    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)
}
