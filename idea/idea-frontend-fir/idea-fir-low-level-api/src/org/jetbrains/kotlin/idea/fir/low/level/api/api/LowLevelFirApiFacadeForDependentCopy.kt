/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.api

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentsOfType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateDepended
import org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveStateImpl
import org.jetbrains.kotlin.idea.fir.low.level.api.util.originalDeclaration
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

object LowLevelFirApiFacadeForDependentCopy {

    private fun KtDeclaration.canBeEnclosingDeclaration(): Boolean = when (this) {
        is KtNamedFunction -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtProperty -> isTopLevel || containingClassOrObject?.isLocal == false
        is KtClassOrObject -> !isLocal
        is KtTypeAlias -> isTopLevel() || containingClassOrObject?.isLocal == false
        else -> false
    }

    private fun findEnclosingNonLocalDeclaration(position: KtElement): KtNamedDeclaration? =
        position.parentsOfType<KtNamedDeclaration>().firstOrNull { ktDeclaration ->
            ktDeclaration.canBeEnclosingDeclaration()
        }

    private fun <T : KtElement> locateDeclarationInFileByOffset(offsetElement: T, file: KtFile): T? {
        val elementOffset = offsetElement.textOffset
        val elementAtOffset = file.findElementAt(elementOffset) ?: return null
        return PsiTreeUtil.getParentOfType(elementAtOffset, offsetElement::class.java, false)?.takeIf { it.textOffset == elementOffset }
    }

    private fun recordOriginalDeclaration(targetDeclaration: KtNamedDeclaration, originalDeclaration: KtNamedDeclaration) {
        require(!targetDeclaration.isPhysical)
        require(originalDeclaration.containingKtFile !== targetDeclaration.containingKtFile)
        val originalDeclrationParents = originalDeclaration.parentsOfType<KtDeclaration>().toList()
        val fakeDeclarationParents = targetDeclaration.parentsOfType<KtDeclaration>().toList()
        originalDeclrationParents.zip(fakeDeclarationParents) { original, fake ->
            fake.originalDeclaration = original
        }
    }

    fun getResolveStateForDependedCopy(
        originalState: FirModuleResolveState,
        originalKtFile: KtFile,
        dependencyKtElement: KtElement
    ): FirModuleResolveState {
        require(originalState is FirModuleResolveStateImpl)

        val dependencyNonLocalDeclaration = findEnclosingNonLocalDeclaration(dependencyKtElement)
            ?: error("Cannot find enclosing declaration for ${dependencyKtElement.getElementTextInContext()}")

        val originalNonLocalDeclaration = locateDeclarationInFileByOffset(dependencyNonLocalDeclaration, originalKtFile)
            ?: error("Cannot find original function matching to ${dependencyNonLocalDeclaration.getElementTextInContext()} in $originalKtFile")

        recordOriginalDeclaration(
            targetDeclaration = dependencyNonLocalDeclaration,
            originalDeclaration = originalNonLocalDeclaration
        )

        val originalFirDeclaration = originalNonLocalDeclaration.getOrBuildFirOfType<FirDeclaration>(originalState)
        val copiedFirDeclaration = DeclarationCopyBuilder.createDeclarationCopy(
            originalFirDeclaration = originalFirDeclaration,
            copiedKtDeclaration = dependencyNonLocalDeclaration,
            state = originalState
        )

        val originalFirFile = originalState.getFirFile(originalKtFile)

        return FirModuleResolveStateDepended(copiedFirDeclaration, originalFirFile, originalState)
    }
}
