/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.TestOnly

object NameHighlighter {
    var namesHighlightingEnabled = true
        @TestOnly set
}

fun AnnotationHolder.highlightName(element: PsiElement, attributesKey: TextAttributesKey) {
    if (NameHighlighter.namesHighlightingEnabled) {
        createInfoAnnotation(element, null).textAttributes = attributesKey
    }
}

fun AnnotationHolder.highlightName(textRange: TextRange, attributesKey: TextAttributesKey) {
    if (NameHighlighter.namesHighlightingEnabled) {
        createInfoAnnotation(textRange, null).textAttributes = attributesKey
    }
}
