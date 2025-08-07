/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService.Companion.getReferencesFromProviders
import org.jetbrains.kotlin.psi.psiUtil.parentSubstitute

open class KtElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), KtElement {
    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun toString(): String = node.elementType.toString()

    final override fun accept(visitor: PsiElementVisitor) {
        if (visitor is KtVisitorVoid) {
            accept(visitor, null)
        } else {
            visitor.visitElement(this)
        }
    }

    override fun getContainingKtFile(): KtFile {
        val file = containingFile
        if (file !is KtFile) {
            val fileString = if (file != null && file.isValid) (" " + file.text) else ""
            throw IllegalStateException(
                "KtElement not inside KtFile: " + file + fileString +
                        " for element " + this + " of type " + this.javaClass + " node = " + node
            )
        }
        return file
    }

    override fun <D> acceptChildren(visitor: KtVisitor<Void, D>, data: D) {
        KtPsiUtil.visitChildren(this, visitor, data)
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitKtElement(this, data)
    }

    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }

    override fun getReference(): PsiReference? = references.singleOrNull()

    override fun getReferences(): Array<PsiReference> {
        return getReferencesFromProviders(this)
    }

    override fun getPsiOrParent(): KtElement = this
    override fun getParent(): PsiElement = parentSubstitute ?: super.getParent()
}
