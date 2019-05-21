/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.contracts.description.LazyContractProvider
import org.jetbrains.kotlin.contracts.parsing.isContractCallDescriptor
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.Call
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.isContractDescriptionCallPsiCheck
import org.jetbrains.kotlin.psi.psiUtil.isFirstStatement
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

/*
 * See KT-26386 and KT-30410
 */
fun disableContractsInsideContractsBlock(call: Call, descriptor: CallableDescriptor?, scope: LexicalScope, trace: BindingTrace) {
    call.callElement.safeAs<KtExpression>()?.let { callExpression ->
        if (callExpression.isFirstStatement() && callExpression.isContractDescriptionCallPsiCheck()) {
            if (descriptor?.isContractCallDescriptor() != true) {
                scope.ownerDescriptor
                    .safeAs<FunctionDescriptor>()
                    ?.getUserData(ContractProviderKey)
                    ?.safeAs<LazyContractProvider>()
                    ?.setContractDescription(null)
            } else {
                trace.record(BindingContext.IS_CONTRACT_DECLARATION_BLOCK, callExpression, true)
            }
        }
    }
}