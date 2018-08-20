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

import org.jetbrains.kotlin.config.AnalysisFlag
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.description.ContractDescription
import org.jetbrains.kotlin.contracts.description.ContractProviderKey
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.isOverridable
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isContractDescriptionCallPsiCheck
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind

class ContractParsingServices(val languageVersionSettings: LanguageVersionSettings) {
    fun checkContractAndRecordIfPresent(expression: KtExpression, trace: BindingTrace, scope: LexicalScope, isFirstStatement: Boolean) {
        if (!expression.isContractDescriptionCallPsiCheck()) return // fastpath

        val collector = TraceBasedCollector(trace, expression)
        val callContext = ContractCallContext(expression, isFirstStatement, scope, trace.bindingContext)
        val parsedContract = doCheckContract(collector, callContext)

        collector.flushDiagnostics(parsingFailed = parsedContract == null)

        val contractProviderIfAny = (scope.ownerDescriptor as? FunctionDescriptor)?.getUserData(ContractProviderKey)

        if (collector.hasErrors())
            contractProviderIfAny?.setContractDescription(null)
        else
            contractProviderIfAny?.setContractDescription(parsedContract)
    }

    private fun doCheckContract(collector: ContractParsingDiagnosticsCollector, callContext: ContractCallContext): ContractDescription? {
        val expression = callContext.contractCallExpression
        val bindingContext = callContext.bindingContext

        if (!expression.isContractDescriptionCallPreciseCheck(bindingContext)) return null

        checkFeatureEnabled(collector)
        checkContractAllowedHere(collector, callContext)

        return if (!collector.hasErrors())
            PsiContractParserDispatcher(collector, callContext).parseContract()
        else
            null
    }

    private fun checkFeatureEnabled(collector: ContractParsingDiagnosticsCollector) {
        val isFeatureTurnedOn = languageVersionSettings.supportsFeature(LanguageFeature.AllowContractsForCustomFunctions) ||
                // This condition is here for technical purposes of compiling 1.2-runtime with contracts
                languageVersionSettings.getFlag(AnalysisFlag.Flags.allowKotlinPackage)
        if (!isFeatureTurnedOn) {
            collector.unsupportedFeature(languageVersionSettings)
        }
    }

    private fun checkContractAllowedHere(collector: ContractParsingDiagnosticsCollector, callContext: ContractCallContext) {
        val functionDescriptor = callContext.ownerDescriptor as? FunctionDescriptor
        val scope = callContext.scope

        if (!callContext.isFirstStatement)
            collector.contractNotAllowed("Contract should be the first statement")

        if (functionDescriptor == null)
            collector.contractNotAllowed("Contracts are allowed only for functions")


        if (callContext.ownerDescriptor.containingDeclaration !is PackageFragmentDescriptor
            || scope.kind != LexicalScopeKind.CODE_BLOCK
            || (scope.parent as? LexicalScope)?.kind != LexicalScopeKind.FUNCTION_INNER_SCOPE
        )
            collector.contractNotAllowed("Contracts are allowed only for top-level functions")

        if (functionDescriptor?.isOperator == true) collector.contractNotAllowed("Contracts are not allowed for operator functions")

        if (functionDescriptor?.isSuspend == true) collector.contractNotAllowed("Contracts are not allowed for suspend functions")

        if (functionDescriptor?.isOverridable == true) collector.contractNotAllowed("Contracts are not allowed for open functions")
    }

    private fun KtExpression.isContractDescriptionCallPreciseCheck(context: BindingContext): Boolean =
        getResolvedCall(context)?.resultingDescriptor?.isContractCallDescriptor() ?: false
}

class ContractCallContext(
    val contractCallExpression: KtExpression,
    val isFirstStatement: Boolean,
    val scope: LexicalScope,
    val bindingContext: BindingContext
) {
    val ownerDescriptor: DeclarationDescriptor = scope.ownerDescriptor
    val functionDescriptor: FunctionDescriptor = ownerDescriptor as FunctionDescriptor
}