/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.utils

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.idea.fir.low.level.api.api.FirModuleResolveState
import org.jetbrains.kotlin.idea.fir.low.level.api.api.LowLevelFirApiFacadeForCompletion
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import kotlin.reflect.KClass

internal sealed class EnclosingDeclarationContext {
    abstract val fakeKtEnclosingDeclaration: KtDeclaration
    abstract val originalKtEnclosingDeclaration: KtDeclaration

    companion object {
        private fun KtDeclaration.canBeEnclosingDeclaration(): Boolean = when (this) {
            is KtNamedFunction -> isTopLevel || containingClassOrObject?.isLocal == false
            is KtProperty -> isTopLevel || containingClassOrObject?.isLocal == false
            is KtClassOrObject -> !isLocal
            is KtTypeAlias -> isTopLevel() || containingClassOrObject?.isLocal == false
            else -> false
        }

        fun detect(originalFile: KtFile, positionInFakeFile: KtElement): EnclosingDeclarationContext {
            val fakeKtDeclaration = positionInFakeFile.parentsOfType<KtNamedDeclaration>().firstOrNull { ktDeclaration ->
                ktDeclaration.canBeEnclosingDeclaration()
            }
            if (fakeKtDeclaration != null) {
                val originalDeclaration = findMatchingElementInCopy(fakeKtDeclaration, originalFile)
                    ?: error("Cannot find original declaration matching to ${fakeKtDeclaration.getElementTextInContext()} in $originalFile")
                recordOriginalDeclaration(originalDeclaration, fakeKtDeclaration)
                return EnclosingDeclarationContextImpl(fakeKtDeclaration, originalDeclaration)
            }

            error("Cannot find enclosing declaration for ${positionInFakeFile.getElementTextInContext()}")
        }

        private fun recordOriginalDeclaration(originalDeclaration: KtNamedDeclaration, fakeDeclaration: KtNamedDeclaration) {
            require(!fakeDeclaration.isPhysical)
            require(originalDeclaration.containingKtFile !== fakeDeclaration.containingKtFile)
            val originalDeclrationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
            val fakeDeclarationParents = fakeDeclaration.parentsOfType<KtDeclaration>().toList()

            originalDeclrationParents.zip(fakeDeclarationParents) { original, fake ->
                fake.originalDeclaration = original
            }
        }
    }
}

internal class EnclosingDeclarationContextImpl(
    override val fakeKtEnclosingDeclaration: KtDeclaration,
    override val originalKtEnclosingDeclaration: KtDeclaration
) : EnclosingDeclarationContext()


internal fun EnclosingDeclarationContext.recordCompletionContext(originalFirFile: FirFile, firResolveState: FirModuleResolveState) {
    LowLevelFirApiFacadeForCompletion.recordCompletionContextForDeclaration(
        originalFirFile,
        fakeKtEnclosingDeclaration,
        originalKtEnclosingDeclaration,
        state = firResolveState,
        fakeContainingFile = fakeKtEnclosingDeclaration.containingKtFile
    )
}

fun <T : KtElement> findMatchingElementInCopy(element: T, copy: KtFile): T? {
    val elementOffset = element.textOffset
    val elementAtOffset = copy.findElementAt(elementOffset) ?: return null
    return PsiTreeUtil.getParentOfType(elementAtOffset, element::class.java, false)?.takeIf { it.textOffset == elementOffset }
}