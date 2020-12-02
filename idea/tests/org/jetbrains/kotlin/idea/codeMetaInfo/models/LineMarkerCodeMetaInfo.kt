/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeMetaInfo.models

import com.intellij.codeInsight.daemon.LineMarkerInfo
import org.jetbrains.kotlin.codeMetaInfo.model.CodeMetaInfo
import org.jetbrains.kotlin.idea.codeMetaInfo.renderConfigurations.LineMarkerRenderConfiguration

class LineMarkerCodeMetaInfo(
    override val renderConfiguration: LineMarkerRenderConfiguration,
    val lineMarker: LineMarkerInfo<*>
) : CodeMetaInfo {
    override val start: Int
        get() = lineMarker.startOffset
    override val end: Int
        get() = lineMarker.endOffset

    override val tag: String
        get() = renderConfiguration.getTag()

    override val platforms: MutableList<String> = mutableListOf()

    override fun asString(): String = renderConfiguration.asString(this)
}
