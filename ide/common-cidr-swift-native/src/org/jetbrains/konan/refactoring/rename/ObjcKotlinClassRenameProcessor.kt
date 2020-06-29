/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.konan.resolve.symbols.KtSymbolPsiWrapper

class ObjcKotlinClassRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is KtSymbolPsiWrapper
    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? = (element as? KtSymbolPsiWrapper)?.psi
}