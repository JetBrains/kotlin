/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.components.psiDeclarationProvider

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement

object TestPsiElementRenderer {
    fun render(psiElement: PsiElement): String = when (psiElement) {
        is KtElement -> psiElement.text
        else -> psiElement.toString()
    }
}