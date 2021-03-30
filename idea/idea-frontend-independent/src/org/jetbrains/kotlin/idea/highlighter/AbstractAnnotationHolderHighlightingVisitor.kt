/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange

abstract class AbstractAnnotationHolderHighlightingVisitor protected constructor(private val holder: AnnotationHolder): AbstractHighlightingVisitor() {
    override fun createInfoAnnotation(textRange: TextRange, message: String?, textAttributes: TextAttributesKey?) {
        (message?.let { holder.newAnnotation(HighlightSeverity.INFORMATION, it) }
            ?: holder.newSilentAnnotation(HighlightSeverity.INFORMATION))
            .range(textRange)
            .also { builder -> textAttributes?.let { builder.textAttributes(it) } }
            .create()
    }
}