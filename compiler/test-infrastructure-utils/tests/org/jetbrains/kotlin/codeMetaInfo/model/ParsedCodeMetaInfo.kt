/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codeMetaInfo.model

import org.jetbrains.kotlin.codeMetaInfo.renderConfigurations.ParsedCodeMetaInfoRenderConfiguration

class ParsedCodeMetaInfo(
    override val start: Int,
    override val end: Int,
    override val attributes: MutableList<String>,
    override val tag: String,
    val description: String?
) : CodeMetaInfo {
    override val renderConfiguration = ParsedCodeMetaInfoRenderConfiguration

    override fun asString(): String = renderConfiguration.asString(this)

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is CodeMetaInfo) return false
        return this.tag == other.tag && this.start == other.start && this.end == other.end
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        result = 31 * result + tag.hashCode()
        return result
    }

    fun copy(): ParsedCodeMetaInfo {
        return ParsedCodeMetaInfo(start, end, attributes.toMutableList(), tag, description)
    }
}

