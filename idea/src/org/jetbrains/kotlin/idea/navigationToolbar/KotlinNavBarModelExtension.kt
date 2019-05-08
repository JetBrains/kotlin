/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigationToolbar

import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.idea.KotlinIconProvider
import org.jetbrains.kotlin.psi.KtFile

class KotlinNavBarModelExtension : AbstractNavBarModelExtension() {
    override fun getPresentableText(`object`: Any?): String? {
        return null
    }

    override fun adjustElement(psiElement: PsiElement?): PsiElement? {
        val containingFile = psiElement?.containingFile as? KtFile ?: return psiElement
        if (containingFile.isScript()) return psiElement
        return KotlinIconProvider.getSingleClass(containingFile)?.toLightClass() ?: psiElement
    }
}
