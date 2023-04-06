/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.tree

import com.intellij.lang.LighterASTNode
import com.intellij.lang.TreeBackedLighterAST
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.KtSourceElementKind
import org.jetbrains.kotlin.WrappedTreeStructure
import org.jetbrains.kotlin.analysis.utils.errors.buildErrorWithAttachment
import org.jetbrains.kotlin.analysis.utils.errors.withVirtualFileEntry
import org.jetbrains.kotlin.fir.builder.FirPsiSourceElementFactory

internal object LLFirPsiSourceElementWithSmartPointerFactory : FirPsiSourceElementFactory() {
    override fun createSourceElement(psi: PsiElement, kind: KtSourceElementKind): KtPsiSourceElement {
        val pointer = SmartPointerManager.createPointer(psi)
        return when (kind) {
            KtRealSourceElementKind -> KtPsiPointerBasedSourceElement.KtPsiPointerBasedRealSourceElement(pointer)
            is KtFakeSourceElementKind -> KtPsiPointerBasedSourceElement.KtPsiPointerBasedFakeSourceElement(pointer, kind)
        }
    }
}


private sealed class KtPsiPointerBasedSourceElement(
    protected val pointer: SmartPsiElementPointer<PsiElement>
) : KtPsiSourceElement() {
    override val psi: PsiElement
        get() {
            return pointer.element
                ?: buildErrorWithAttachment("Cannot restore PSI element") {
                    withEntry("pointer", pointer) { it.toString() }
                    withVirtualFileEntry("virtualFile", pointer.virtualFile)
                }
        }

    override val lighterASTNode: LighterASTNode get() = TreeBackedLighterAST.wrap(psi.node)
    override val treeStructure: FlyweightCapableTreeStructure<LighterASTNode> get() = WrappedTreeStructure(psi.containingFile)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (this::class.java != other?.let { it::class.java }) return false
        check(other is KtPsiPointerBasedSourceElement)
        return this.psi == other.psi && this.kind == other.kind
    }

    override fun hashCode(): Int = 31 * kind.hashCode() + psi.hashCode()

    class KtPsiPointerBasedRealSourceElement(
        pointer: SmartPsiElementPointer<PsiElement>
    ) : KtPsiPointerBasedSourceElement(pointer) {
        override val kind: KtSourceElementKind get() = KtRealSourceElementKind

        override fun realElement(): KtSourceElement = this
        override fun fakeElement(newKind: KtFakeSourceElementKind): KtSourceElement = KtPsiPointerBasedFakeSourceElement(pointer, newKind)
    }

    class KtPsiPointerBasedFakeSourceElement(
        pointer: SmartPsiElementPointer<PsiElement>,
        override val kind: KtSourceElementKind,
    ) : KtPsiPointerBasedSourceElement(pointer) {

        override fun realElement(): KtSourceElement = KtPsiPointerBasedRealSourceElement(pointer)

        override fun fakeElement(newKind: KtFakeSourceElementKind): KtSourceElement {
            if (kind == newKind) return this
            return KtPsiPointerBasedFakeSourceElement(pointer, newKind)
        }
    }
}