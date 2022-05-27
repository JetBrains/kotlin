/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.references.fe10

import org.jetbrains.kotlin.references.fe10.base.KtFe10Reference
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.references.KtInvokeFunctionReference
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

class KtFe10InvokeFunctionReference(expression: KtCallExpression) : KtInvokeFunctionReference(expression), KtFe10Reference {

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

    override fun isReferenceToImportAlias(alias: KtImportAlias): Boolean {
        return super<KtFe10Reference>.isReferenceToImportAlias(alias)
    }
}
