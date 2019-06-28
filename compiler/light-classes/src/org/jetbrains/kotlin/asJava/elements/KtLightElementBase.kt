/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.elements

import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtElement

abstract class KtLightElementBase(private val parent: PsiElement) : LightElement(parent.manager, KotlinLanguage.INSTANCE) {
    override fun toString() = "${this.javaClass.simpleName} of $parent"
    override fun getParent(): PsiElement = parent

    abstract val kotlinOrigin: KtElement?

    override fun getText() = kotlinOrigin?.text ?: ""
    override fun getTextRange() = kotlinOrigin?.textRange ?: TextRange.EMPTY_RANGE
    override fun getTextOffset() = kotlinOrigin?.textOffset ?: 0
    override fun getStartOffsetInParent() = kotlinOrigin?.startOffsetInParent ?: 0
    override fun isWritable() = kotlinOrigin?.isWritable ?: false
    override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: this
    override fun getUseScope() = kotlinOrigin?.useScope ?: super.getUseScope()
    override fun getContainingFile() = parent.containingFile
    override fun getPresentation() = (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }
    override fun isValid() = parent.isValid && (kotlinOrigin?.isValid != false)
    override fun findElementAt(offset: Int) = kotlinOrigin?.findElementAt(offset)
    override fun isEquivalentTo(another: PsiElement?): Boolean =
        super.isEquivalentTo(another) || kotlinOrigin?.isEquivalentTo(another) == true
}
