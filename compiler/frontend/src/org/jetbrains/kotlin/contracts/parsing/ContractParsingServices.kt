/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.contracts.parsing

import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.ContractDescription
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.contracts.description.LazyContractProvider
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isContractDescriptionCallPsiCheck
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.storage.StorageManager

class ContractParsingServices(val languageVersionSettings: LanguageVersionSettings, private val storageManager: StorageManager) {
    /**
     * ! IMPORTANT NOTICE !
     *
     * This function has very important non-obvious implicit contract:
     * it *must* call [org.jetbrains.kotlin.contracts.description.LazyContractProvider.setContractDescription]
     * if FunctionDescriptor had [LazyContractProvider] in the user data.
     *
     * Otherwise, it may lead to inconsistent resolve state and failed assertions
     */
    fun checkContractAndRecordIfPresent(expression: KtExpression, trace: BindingTrace, scope: LexicalScope) {
        // Fastpath. Note that it doesn't violates invariant described in KDoc, because 'isContractDescriptionCallPsiCheck'
        // is a *necessary* (but not sufficient, actually) condition for presence of 'LazyContractProvider'
        if (!expression.isContractDescriptionCallPsiCheck()) return

        val callContext = ContractCallContext(expression, scope, trace)
        val contractProviderIfAny =
            (scope.ownerDescriptor as? FunctionDescriptor)?.getUserData(ContractProviderKey) as? LazyContractProvider?
        var resultingContractDescription: ContractDescription? = null

        try {
            if (!callContext.isContractDescriptionCallPreciseCheck()) return
            resultingContractDescription = parseContractAndReportErrors(callContext)
        } finally {
            contractProviderIfAny?.setContractDescription(resultingContractDescription)
        }
    }

    private fun ContractCallContext.isContractDescriptionCallPreciseCheck(): Boolean =
        contractCallExpression.isContractDescriptionCallPreciseCheck(bindingContext)

    /**
     * This function deals with some call that is guaranteed to resolve to 'contract' from stdlib, so,
     * ideally, it should satisfy following condition: null returned <=> at least one error was reported
     */
    private fun parseContractAndReportErrors(callContext: ContractCallContext): ContractDescription? {
        val collector = TraceBasedCollector(callContext)

        try {
            checkFeatureEnabled(collector)

            val contractNotAllowed = callContext.bindingContext[BindingContext.CONTRACT_NOT_ALLOWED, callContext.contractCallExpression] == true
            // Small optimization: do not even try to parse contract if we already have errors
            if (collector.hasErrors() || contractNotAllowed) return null

            val parsedContract = PsiContractParserDispatcher(collector, callContext, storageManager).parseContract()

            // Make sure that at least generic error will be reported if we couldn't parse contract
            // (null returned => at least one error was reported)
            if (parsedContract == null) collector.addFallbackErrorIfNecessary()

            // Make sure that we don't return non-null value if there were some errors
            // (null returned <= at least one error was reported)
            return parsedContract?.takeUnless { collector.hasErrors() }
        } finally {
            collector.flushDiagnostics()
        }
    }

    private fun checkFeatureEnabled(collector: ContractParsingDiagnosticsCollector) {
        val isFeatureTurnedOn = languageVersionSettings.supportsFeature(LanguageFeature.AllowContractsForCustomFunctions) ||
                // This condition is here for technical purposes of compiling 1.2-runtime with contracts
                languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)
        if (!isFeatureTurnedOn) {
            collector.unsupportedFeature(languageVersionSettings)
        }
    }

    private fun KtExpression.isContractDescriptionCallPreciseCheck(context: BindingContext): Boolean =
        getResolvedCall(context)?.resultingDescriptor?.isContractCallDescriptor() ?: false
}

class ContractCallContext(
    val contractCallExpression: KtExpression,
    val scope: LexicalScope,
    val trace: BindingTrace
) {
    val ownerDescriptor: DeclarationDescriptor = scope.ownerDescriptor
    val functionDescriptor: FunctionDescriptor = ownerDescriptor as FunctionDescriptor
    val bindingContext: BindingContext = trace.bindingContext
}