/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall

class KtInvokeFunctionReferenceDescriptorsImpl(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KtDescriptorsBasedReference {
    override fun isReferenceTo(element: PsiElement): Boolean =
        super<KtDescriptorsBasedReference>.isReferenceTo(element)


    override fun getTargetDescriptors(context: BindingContext): Collection<DeclarationDescriptor> {
        val call = element.getCall(context)
        val resolvedCall = call.getResolvedCall(context)
        return when {
            resolvedCall is VariableAsFunctionResolvedCall ->
                setOf<DeclarationDescriptor>((resolvedCall as VariableAsFunctionResolvedCall).functionCall.candidateDescriptor)
            call != null && resolvedCall != null && call.callType == Call.CallType.INVOKE ->
                setOf<DeclarationDescriptor>(resolvedCall.candidateDescriptor)
            else ->
                emptyList()
        }
    }

    override fun doRenameImplicitConventionalCall(newName: String?): KtExpression =
        renameImplicitConventionalCall(newName)
}
