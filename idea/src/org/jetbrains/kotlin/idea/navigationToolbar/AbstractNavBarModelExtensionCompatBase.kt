/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigationToolbar

import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.KotlinIconProviderBase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile

// FIX ME WHEN BUNCH 193 REMOVED
abstract class AbstractNavBarModelExtensionCompatBase : StructureAwareNavBarModelExtension() {

    protected abstract fun adjustElementImpl(psiElement: PsiElement?): PsiElement?

    override fun adjustElement(psiElement: PsiElement): PsiElement? =
        adjustElementImpl(psiElement)

    override val language: Language
        get() = KotlinLanguage.INSTANCE

    override fun acceptParentFromModel(psiElement: PsiElement?): Boolean {
        if (psiElement is KtFile) {
            return KotlinIconProviderBase.getSingleClass(psiElement) == null
        }
        return true
    }
}