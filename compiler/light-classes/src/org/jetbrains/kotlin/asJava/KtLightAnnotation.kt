/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiAnnotationOwner
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.psi.KtAnnotationEntry

class KtLightAnnotation(
        private val delegate: PsiAnnotation,
        private val originalElement: KtAnnotationEntry,
        private val owner: PsiAnnotationOwner
) : PsiAnnotation by delegate, KtLightElement<KtAnnotationEntry, PsiAnnotation> {
    override fun getDelegate() = delegate
    override fun getOrigin() = originalElement

    override fun getName() = null
    override fun setName(newName: String) = throw IncorrectOperationException()

    override fun getOwner() = owner

    override fun getText() = originalElement.text ?: ""
    override fun getTextRange() = originalElement.textRange ?: TextRange.EMPTY_RANGE

    override fun getParent() = owner as? PsiElement

    override fun toString() = "@$qualifiedName"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        return originalElement == (other as KtLightAnnotation).originalElement
    }

    override fun hashCode() = originalElement.hashCode()
}
