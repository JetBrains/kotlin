/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtConstructorDelegationReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets

class KtConstructorDelegationReferenceDescriptorsImpl(
    expression: KtConstructorDelegationReferenceExpression
) : KtConstructorDelegationReference(expression), KtDescriptorsBasedReference {

    override fun isReferenceTo(element: PsiElement): Boolean =
        super<KtDescriptorsBasedReference>.isReferenceTo(element)

    override fun getTargetDescriptors(context: BindingContext) = expression.getReferenceTargets(context)
}