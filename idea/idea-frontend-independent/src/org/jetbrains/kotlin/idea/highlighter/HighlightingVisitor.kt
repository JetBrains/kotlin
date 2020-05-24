/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class HighlightingVisitor protected constructor(
    private val holder: AnnotationHolder
) : KtVisitorVoid() {

    protected fun createInfoAnnotation(element: PsiElement, textAttributes: TextAttributesKey, message: String? = null) {
        createInfoAnnotation(element.textRange, textAttributes, message)
    }

    protected fun createInfoAnnotation(range: TextRange, textAttributes: TextAttributesKey, message: String? = null) {
        createInfoAnnotation(range, message).textAttributes = textAttributes
    }

    protected fun createInfoAnnotation(element: PsiElement, message: String? = null): Annotation =
        createInfoAnnotation(element.textRange, message)

    protected fun createInfoAnnotation(textRange: TextRange, message: String? = null): Annotation =
        holder.createInfoAnnotation(textRange, message)

    protected fun highlightName(element: PsiElement, attributesKey: TextAttributesKey, message: String? = null) {
        if (NameHighlighter.namesHighlightingEnabled && !element.textRange.isEmpty) {
            createInfoAnnotation(element, attributesKey, message)
        }
    }

    protected fun highlightName(textRange: TextRange, attributesKey: TextAttributesKey, message: String? = null) {
        if (NameHighlighter.namesHighlightingEnabled) {
            createInfoAnnotation(textRange, attributesKey, message)
        }
    }
}
