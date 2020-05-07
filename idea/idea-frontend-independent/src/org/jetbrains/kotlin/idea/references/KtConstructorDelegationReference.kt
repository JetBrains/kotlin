/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression

abstract class KtConstructorDelegationReference(
    expression: KtConstructorDelegationReferenceExpression
) : KtSimpleReference<KtConstructorDelegationReferenceExpression>(expression) {
    override fun getRangeInElement(): TextRange {
        return TextRange(0, element.textLength)
    }

    override val resolvesByNames: Collection<Name>
        get() = emptyList()

    override fun handleElementRename(newElementName: String): PsiElement? {
        // Class rename never affects this reference, so there is no need to fail with exception
        return expression
    }
}