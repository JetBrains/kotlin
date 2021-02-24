/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_GET
import org.jetbrains.kotlin.resolve.BindingContext.INDEXED_LVALUE_SET

internal class KtArrayAccessReferenceDescriptorsImpl(
    expression: KtArrayAccessExpression
) : KtArrayAccessReference(expression), KtDescriptorsBasedReference {
    override fun isReferenceTo(element: PsiElement): Boolean =
        super<KtDescriptorsBasedReference>.isReferenceTo(element)

    override fun moveFunctionLiteralOutsideParentheses(callExpression: KtCallExpression) {
        callExpression.moveFunctionLiteralOutsideParentheses()
    }

    override fun canMoveLambdaOutsideParentheses(callExpression: KtCallExpression): Boolean =
        callExpression.canMoveLambdaOutsideParentheses()

    override fun doRenameImplicitConventionalCall(newName: String?): KtExpression =
        renameImplicitConventionalCall(newName)

    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val getFunctionDescriptor = context[INDEXED_LVALUE_GET, expression]?.candidateDescriptor
        val setFunctionDescriptor = context[INDEXED_LVALUE_SET, expression]?.candidateDescriptor
        return listOfNotNull(getFunctionDescriptor, setFunctionDescriptor)
    }
}
