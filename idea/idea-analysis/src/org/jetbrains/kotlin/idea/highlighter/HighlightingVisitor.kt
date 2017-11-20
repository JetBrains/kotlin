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

import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class HighlightingVisitor protected constructor(
        private val holder: AnnotationHolder
) : KtVisitorVoid() {

    protected fun createInfoAnnotation(element: PsiElement, message: String? = null): Annotation =
            createInfoAnnotation(element.textRange, message)

    protected fun createInfoAnnotation(textRange: TextRange, message: String? = null): Annotation =
            holder.createInfoAnnotation(textRange, message)

    protected fun highlightName(element: PsiElement, attributesKey: TextAttributesKey, message: String? = null) {
        if (NameHighlighter.namesHighlightingEnabled) {
            createInfoAnnotation(element, message).textAttributes = attributesKey
        }
    }

    protected fun highlightName(textRange: TextRange, attributesKey: TextAttributesKey, message: String? = null) {
        if (NameHighlighter.namesHighlightingEnabled) {
            createInfoAnnotation(textRange, message).textAttributes = attributesKey
        }
    }

    protected fun applyHighlighterExtensions(element: PsiElement, descriptor: DeclarationDescriptor): Boolean {
        if (!NameHighlighter.namesHighlightingEnabled) return false
        for (extension in Extensions.getExtensions(HighlighterExtension.EP_NAME)) {
            val key = extension.highlightReference(element, descriptor)
            if (key != null) {
                highlightName(element, key)
                return true
            }
        }
        return false
    }
}
