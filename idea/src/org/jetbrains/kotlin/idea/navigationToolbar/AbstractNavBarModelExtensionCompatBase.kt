/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigationToolbar

import com.intellij.ide.navigationToolbar.AbstractNavBarModelExtension
import com.intellij.psi.PsiElement

// BUNCH 201
abstract class AbstractNavBarModelExtensionCompatBase : AbstractNavBarModelExtension() {

    protected abstract fun adjustElementImpl(psiElement: PsiElement?): PsiElement?

    override fun adjustElement(psiElement: PsiElement): PsiElement? =
        adjustElementImpl(psiElement)
}