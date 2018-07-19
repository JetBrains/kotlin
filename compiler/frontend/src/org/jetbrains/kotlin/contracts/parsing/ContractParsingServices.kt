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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isContractDescriptionCallFastCheck
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind

class ContractParsingServices(val languageVersionSettings: LanguageVersionSettings) {
    fun checkContractAndRecordIfPresent(expression: KtExpression, trace: BindingTrace, scope: LexicalScope, isFirstStatement: Boolean) {
        val ownerDescriptor = scope.ownerDescriptor
        if (!isContractDescriptionCallFastCheck(expression) || ownerDescriptor !is FunctionDescriptor) return
        val contractProvider = ownerDescriptor.getUserData(ContractProviderKey) ?: return

        val isFeatureTurnedOn = languageVersionSettings.supportsFeature(LanguageFeature.AllowContractsForCustomFunctions) ||
                // This condition is here for technical purposes of compiling 1.2-runtime with contracts
                languageVersionSettings.getFlag(AnalysisFlag.Flags.allowKotlinPackage)

        val contractDescriptor = when {
            !isContractDescriptionCallPreciseCheck(expression, trace.bindingContext) -> null

            !isFeatureTurnedOn -> {
                trace.report(
                    Errors.UNSUPPORTED_FEATURE.on(
                        expression,
                        LanguageFeature.AllowContractsForCustomFunctions to languageVersionSettings
                    )
                )
                null
            }

            !isContractAllowedHere(scope) || !isFirstStatement -> {
                trace.report(Errors.CONTRACT_NOT_ALLOWED.on(expression))
                null
            }

            else -> parseContract(expression, trace, ownerDescriptor)
        }

        contractProvider.setContractDescription(contractDescriptor)
    }

    private fun parseContract(expression: KtExpression?, trace: BindingTrace, ownerDescriptor: FunctionDescriptor): ContractDescription? =
        PsiContractParserDispatcher(trace, this).parseContract(expression, ownerDescriptor)

    internal fun isContractDescriptionCall(expression: KtExpression, context: BindingContext): Boolean =
        isContractDescriptionCallFastCheck(expression) && isContractDescriptionCallPreciseCheck(expression, context)

    private fun isContractAllowedHere(scope: LexicalScope): Boolean =
        scope.kind == LexicalScopeKind.CODE_BLOCK && (scope.parent as? LexicalScope)?.kind == LexicalScopeKind.FUNCTION_INNER_SCOPE

    private fun isContractDescriptionCallPreciseCheck(expression: KtExpression, context: BindingContext): Boolean =
        expression.getResolvedCall(context)?.resultingDescriptor?.isContractCallDescriptor() ?: false
}