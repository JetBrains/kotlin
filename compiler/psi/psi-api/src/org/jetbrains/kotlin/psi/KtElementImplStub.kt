/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.psi

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.StubBasedPsiElement
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService.Companion.getReferencesFromProviders
import org.jetbrains.kotlin.psi.psiUtil.parentSubstitute
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementType

abstract class KtElementImplStub<T : StubElement<*>> : StubBasedPsiElementBase<T>, KtElement, StubBasedPsiElement<T> {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    override fun getLanguage(): Language = KotlinLanguage.INSTANCE

    override fun toString(): String = elementType.toString()

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
            // KtElementImpl.copy() might be the reason for this exception
            var fileString = ""
            if (file.isValid) {
                try {
                    fileString = " " + file.text
                } catch (_: Exception) {
                    // ignore when failed to get file text
                }
            }
            // getNode() will fail if getContainingFile() returns not PsiFileImpl instance
            val nodeString = (if (file is PsiFileImpl) (" node = " + getNode()) else "")

            throw IllegalStateException(
                "KtElement not inside KtFile: " +
                        file + fileString + " of type " + file.javaClass +
                        " for element " + this + " of type " + this.javaClass + nodeString
            )
        }
        return file
    }

    override fun <D> acceptChildren(visitor: KtVisitor<Void, D>, data: D) {
        var child = firstChild
        while (child != null) {
            if (child is KtElement) {
                child.accept(visitor, data)
            }
            child = child.nextSibling
        }
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitKtElement(this, data)
    }

    override fun delete() {
        this.deleteSemicolon()
        super.delete()
    }

    override fun getReference(): PsiReference? = references.firstOrNull()

    override fun getReferences(): Array<PsiReference> = getReferencesFromProviders(this)

    fun <PsiT : KtElementImplStub<*>, StubT : StubElement<*>> getStubOrPsiChildrenAsList(
        elementType: KtStubElementType<StubT, PsiT>
    ): List<PsiT> = getStubOrPsiChildren(elementType, elementType.getArrayFactory()).asList()

    override fun getPsiOrParent(): KtElement = this

    override fun getParent(): PsiElement = parentSubstitute ?: super.getParent()
}
