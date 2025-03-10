/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.elements

import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtElement

abstract class KtLightElementBase(private var parent: PsiElement) : LightElement(parent.manager, KotlinLanguage.INSTANCE) {
    override fun toString() = "${this.javaClass.simpleName} of $parent"
    override fun getParent(): PsiElement = parent

    abstract val kotlinOrigin: KtElement?

    @Deprecated("Hack for ULC", level = DeprecationLevel.ERROR)
    fun setParent(newParent: PsiElement) {
        parent = newParent
    }

    override fun getText() = kotlinOrigin?.text ?: ""
    override fun getTextRange(): TextRange? = kotlinOrigin?.textRange
    override fun getTextOffset(): Int = kotlinOrigin?.textOffset ?: -1
    override fun getStartOffsetInParent(): Int = kotlinOrigin?.startOffsetInParent ?: -1
    override fun isWritable() = kotlinOrigin?.isWritable ?: false
    override fun getNavigationElement() = kotlinOrigin?.navigationElement ?: this
    override fun getUseScope() = kotlinOrigin?.useScope ?: super.getUseScope()
    override fun getContainingFile() = parent.containingFile
    override fun getPresentation() = (kotlinOrigin ?: this).let { ItemPresentationProviders.getItemPresentation(it) }
    override fun isValid() = parent.isValid && (kotlinOrigin?.isValid != false)
    override fun findElementAt(offset: Int) = kotlinOrigin?.findElementAt(offset)
    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (super.isEquivalentTo(another)) {
            return true
        }

        val origin = kotlinOrigin ?: return false
        return origin.isEquivalentTo(another) ||
                (another is KtLightElementBase && origin.isEquivalentTo(another.kotlinOrigin))
    }
}