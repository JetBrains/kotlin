/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.contracts.interpretation.ContractInterpretationDispatcher
import org.jetbrains.kotlin.contracts.interpretation.EffectDeclarationInterpreter
import org.jetbrains.kotlin.contracts.model.ExtensionEffect
import org.jetbrains.kotlin.contracts.model.functors.ExtensionSubstitutor
import org.jetbrains.kotlin.contracts.model.visitors.ExtensionReducerConstructor
import org.jetbrains.kotlin.contracts.parsing.ContractCallContext
import org.jetbrains.kotlin.contracts.parsing.ContractParsingDiagnosticsCollector
import org.jetbrains.kotlin.contracts.parsing.ExtensionParserDispatcher
import org.jetbrains.kotlin.contracts.parsing.PsiContractParserDispatcher
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

interface ContractsExtension {
    companion object : ProjectExtensionDescriptor<ContractsExtension>(
        "org.jetbrains.kotlin.contractsExtension",
        ContractsExtension::class.java
    )

    val id: String

    fun getPsiParserDispatcher(
        collector: ContractParsingDiagnosticsCollector,
        callContext: ContractCallContext,
        dispatcher: PsiContractParserDispatcher
    ): ExtensionParserDispatcher

    fun getEffectDeclarationInterpreterConstructor(): (ContractInterpretationDispatcher) -> EffectDeclarationInterpreter

    fun getExtensionReducerConstructor(): ExtensionReducerConstructor

    fun getExtensionSubstitutor(): ExtensionSubstitutor

    fun emptyBindingContextData(): ExtensionBindingContextData

    fun collectDefiniteInvocations(
        effect: ExtensionEffect,
        resolvedCall: ResolvedCall<*>,
        bindingContext: BindingContext
    ): ContractsInfoForInvocation?

    fun analyzeFunction(
        function: KtFunction,
        pseudocode: Pseudocode,
        bindingContext: BindingContext,
        diagnosticSink: DiagnosticSink
    )
}

data class ContractsInfoForInvocation(val expression: KtExpression, val data: ExtensionBindingContextData)