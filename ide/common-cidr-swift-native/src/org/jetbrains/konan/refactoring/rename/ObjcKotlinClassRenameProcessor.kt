package org.jetbrains.konan.refactoring.rename

import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import org.jetbrains.konan.resolve.symbols.KtSymbolPsiWrapper

class ObjcKotlinClassRenameProcessor : RenamePsiElementProcessor() {
    override fun canProcessElement(element: PsiElement): Boolean = element is KtSymbolPsiWrapper
    override fun substituteElementToRename(element: PsiElement, editor: Editor?): PsiElement? = (element as? KtSymbolPsiWrapper)?.psi
}