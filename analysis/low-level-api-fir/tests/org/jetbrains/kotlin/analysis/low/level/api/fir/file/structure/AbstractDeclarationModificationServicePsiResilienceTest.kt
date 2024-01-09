/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiModifiablePsiTest
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * In the IDE, [LLFirDeclarationModificationService] might be called with PSI elements received via PSI tree change events. Because such
 * elements may currently be in the process of modification, they may be inconsistent. This test ensures that the declaration modification
 * service can handle such inconsistent PSI, without throwing exceptions.
 */
abstract class AbstractDeclarationModificationServicePsiResilienceTest : AbstractLowLevelApiModifiablePsiTest() {
    protected abstract fun modifySelectedElement(element: PsiElement)

    override fun doTestWithPsiModification(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider.getSelectedElement(ktFile)
        modifySelectedElement(selectedElement)

        // The test passes when `LLFirDeclarationModificationService` throws no exceptions.
        LLFirDeclarationModificationService.getInstance(ktFile.project).elementModified(selectedElement)
    }
}

@OptIn(ExperimentalContracts::class)
private inline fun <reified E : PsiElement> assertSelectedElementType(selectedElement: PsiElement) {
    contract { returns() implies (selectedElement is E) }
    if (selectedElement !is E) {
        error("Expected the selected element to be a ${E::class.simpleName}. Selected element: $selectedElement")
    }
}

abstract class AbstractDeclarationModificationServiceCallExpressionCalleeResilienceTest :
    AbstractDeclarationModificationServicePsiResilienceTest() {
    override fun modifySelectedElement(element: PsiElement) {
        assertSelectedElementType<KtCallExpression>(element)

        val calleeExpression = element.calleeExpression
        require(calleeExpression != null) {
            "A consistent call expression should have a callee expression. Expression: $this"
        }
        calleeExpression.delete()
    }
}

abstract class AbstractDeclarationModificationServiceDotQualifiedExpressionReceiverResilienceTest :
    AbstractDeclarationModificationServicePsiResilienceTest() {
    override fun modifySelectedElement(element: PsiElement) {
        assertSelectedElementType<KtDotQualifiedExpression>(element)
        element.checkConsistency()

        element.firstChild.delete()
    }
}

abstract class AbstractDeclarationModificationServiceDotQualifiedExpressionSelectorResilienceTest :
    AbstractDeclarationModificationServicePsiResilienceTest() {
    override fun modifySelectedElement(element: PsiElement) {
        assertSelectedElementType<KtDotQualifiedExpression>(element)
        element.checkConsistency()

        element.firstChild.delete()
        element.operationTokenNode.psi.delete()
    }
}

private fun KtDotQualifiedExpression.checkConsistency() {
    require(children.size == 2) {
        "A consistent dot-qualified expression should have two children. Expression: $this"
    }
}

abstract class AbstractDeclarationModificationServicePropertyDeclarationInitializerResilienceTest :
    AbstractDeclarationModificationServicePsiResilienceTest() {
    override fun modifySelectedElement(element: PsiElement) {
        assertSelectedElementType<KtProperty>(element)

        val initializer = element.initializer
        require(initializer != null) {
            "The property declaration is expected to have an initializer. Property declaration: $this"
        }
        initializer.delete()
    }
}
