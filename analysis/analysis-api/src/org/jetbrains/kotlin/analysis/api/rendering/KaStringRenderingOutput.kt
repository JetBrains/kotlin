/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi

@KaExperimentalApi
internal class KaStringRenderingOutput(private val indentationUnit: String) : KaRenderingOutput {
    private val builder: StringBuilder = StringBuilder()
    private var indentLevel: Int = 0
    private var atLineStart: Boolean = true

    override fun append(text: String, attributes: Set<KaTextAttribute>): KaRenderingOutput {
        if (text.isEmpty()) return this
        indentIfNeeded()
        builder.append(text)
        return this
    }

    override fun indent(): KaRenderingOutput {
        indentLevel++
        return this
    }

    override fun unindent(): KaRenderingOutput {
        require(indentLevel > 0) { "Unbalanced indent()/unindent() calls." }
        indentLevel--
        return this
    }

    override fun newLine(): KaRenderingOutput {
        // Avoid trailing whitespace at the end of the line.
        while (builder.isNotEmpty() && builder.last() == ' ') {
            builder.deleteCharAt(builder.length - 1)
        }
        builder.append('\n')
        atLineStart = true
        return this
    }

    override fun space(): KaRenderingOutput {
        indentIfNeeded()
        builder.append(' ')
        return this
    }

    private fun indentIfNeeded() {
        if (atLineStart) {
            repeat(indentLevel) { builder.append(indentationUnit) }
            atLineStart = false
        }
    }

    override fun toString(): String = builder.toString().trim()
}
