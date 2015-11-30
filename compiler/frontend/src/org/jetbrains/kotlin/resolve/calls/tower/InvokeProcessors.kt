/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.tower

import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.createSynthesizedInvokes
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*


internal abstract class AbstractInvokeTowerProcessor<C>(
        protected val functionContext: TowerContext<C>,
        private val variableProcessor: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    // todo optimize it
    private val previousActions = ArrayList<ScopeTowerProcessor<C>.() -> Unit>()
    private val candidateGroups: MutableList<Collection<C>> = ArrayList()

    private val invokeProcessors: MutableList<Collection<VariableInvokeProcessor>> = ArrayList()


    private inner class VariableInvokeProcessor(
            val variableCandidate: C,
            val invokeProcessor: ScopeTowerProcessor<C> = createInvokeProcessor(variableCandidate)
    ): ScopeTowerProcessor<C> by invokeProcessor {
        override fun getCandidatesGroups(): List<Collection<C>>
                = invokeProcessor.getCandidatesGroups().map { candidateGroup ->
                    candidateGroup.map { functionContext.transformCandidate(variableCandidate, it) }
                }
    }

    protected abstract fun createInvokeProcessor(variableCandidate: C): ScopeTowerProcessor<C>

    private fun findVariablesAndCreateNewInvokeCandidates() {
        for (variableCandidates in variableProcessor.getCandidatesGroups()) {
            val successfulVariables = variableCandidates.filter {
                functionContext.getStatus(it).resultingApplicability.isSuccess
            }

            if (successfulVariables.isNotEmpty()) {
                val processors = successfulVariables.map { VariableInvokeProcessor(it) }
                invokeProcessors.add(processors)

                for (previousAction in previousActions) {
                    processors.forEach(previousAction)
                    candidateGroups.addAll(processors.collectCandidateGroups())
                }
            }
        }
    }

    private fun Collection<VariableInvokeProcessor>.collectCandidateGroups(): List<Collection<C>> {
        return when (size) {
            0 -> emptyList()
            1 -> single().getCandidatesGroups()
            // overload on variables see KT-10093 Resolve depends on the order of declaration for variable with implicit invoke

            else -> listOf(this.flatMap { it.getCandidatesGroups().flatten() })
        }
    }

    private fun proceed(action: ScopeTowerProcessor<C>.() -> Unit) {
        candidateGroups.clear()
        previousActions.add(action)

        for (processorsGroup in invokeProcessors) {
            processorsGroup.forEach(action)
            candidateGroups.addAll(processorsGroup.collectCandidateGroups())
        }

        variableProcessor.action()
        findVariablesAndCreateNewInvokeCandidates()
    }

    init { proceed { /* do nothing */ } }

    override fun processTowerLevel(level: ScopeTowerLevel) = proceed { this.processTowerLevel(level) }

    override fun processImplicitReceiver(implicitReceiver: ReceiverValue)
            = proceed { this.processImplicitReceiver(implicitReceiver) }

    override fun getCandidatesGroups(): List<Collection<C>> = SmartList(candidateGroups)
}

// todo KT-9522 Allow invoke convention for synthetic property
internal class InvokeTowerProcessor<C>(
        functionContext: TowerContext<C>,
        private val explicitReceiver: Receiver?
) : AbstractInvokeTowerProcessor<C>(
        functionContext,
        createVariableProcessor(functionContext.contextForVariable(stripExplicitReceiver = false), explicitReceiver)
) {

    // todo filter by operator
    override fun createInvokeProcessor(variableCandidate: C): ScopeTowerProcessor<C> {
        val (variableReceiver, invokeContext) = functionContext.contextForInvoke(variableCandidate, useExplicitReceiver = false)
        return ExplicitReceiverScopeTowerProcessor(invokeContext, variableReceiver, ScopeTowerLevel::getFunctions)
    }
}

internal class InvokeExtensionTowerProcessor<C>(
        functionContext: TowerContext<C>,
        private val explicitReceiver: ReceiverValue?
) : AbstractInvokeTowerProcessor<C>(
        functionContext,
        createVariableProcessor(functionContext.contextForVariable(stripExplicitReceiver = true), explicitReceiver = null)
) {

    override fun createInvokeProcessor(variableCandidate: C): ScopeTowerProcessor<C> {
        val (variableReceiver, invokeContext) = functionContext.contextForInvoke(variableCandidate, useExplicitReceiver = true)
        val invokeDescriptor = functionContext.scopeTower.getExtensionInvokeCandidateDescriptor(variableReceiver)
                               ?: return KnownResultProcessor(emptyList())
        return InvokeExtensionScopeTowerProcessor(invokeContext, invokeDescriptor, explicitReceiver)
    }
}

private class InvokeExtensionScopeTowerProcessor<C>(
        context: TowerContext<C>,
        val invokeCandidateDescriptor: CandidateWithBoundDispatchReceiver<FunctionDescriptor>,
        val explicitReceiver: ReceiverValue?
) : AbstractScopeTowerProcessor<C>(context) {
    override var candidates: Collection<C> = resolve(explicitReceiver)

    private fun resolve(extensionReceiver: ReceiverValue?): Collection<C> {
        if (extensionReceiver == null) return emptyList()

        // todo
        val explicitReceiverKind = if (explicitReceiver != null) ExplicitReceiverKind.BOTH_RECEIVERS else ExplicitReceiverKind.DISPATCH_RECEIVER

        val candidate = context.createCandidate(invokeCandidateDescriptor, explicitReceiverKind, extensionReceiver)
        return listOf(candidate)
    }

    override fun processTowerLevel(level: ScopeTowerLevel) {
        candidates = emptyList()
    }

    // todo optimize
    override fun processImplicitReceiver(implicitReceiver: ReceiverValue) {
        if (explicitReceiver == null) {
            candidates = resolve(implicitReceiver)
        }
        else {
            candidates = emptyList()
        }
    }
}

// todo debug info
private fun ScopeTower.getExtensionInvokeCandidateDescriptor(
        extensionFunctionReceiver: ReceiverValue
): CandidateWithBoundDispatchReceiver<FunctionDescriptor>? {
    if (!KotlinBuiltIns.isExactExtensionFunctionType(extensionFunctionReceiver.type)) return null

    return ReceiverScopeTowerLevel(this, extensionFunctionReceiver).getFunctions(OperatorNameConventions.INVOKE).single().let {
        assert(it.diagnostics.isEmpty())
        val synthesizedInvoke = createSynthesizedInvokes(listOf(it.descriptor)).single()

        // here we don't add SynthesizedDescriptor diagnostic because it should has priority as member
        CandidateWithBoundDispatchReceiverImpl(extensionFunctionReceiver, synthesizedInvoke, listOf())
    }
}

// case 1.(foo())() or (foo())()
internal fun <C> createCallTowerProcessorForExplicitInvoke(
        contextForInvoke: TowerContext<C>,
        expressionForInvoke: ReceiverValue,
        explicitReceiver: ReceiverValue?
): ScopeTowerProcessor<C> {
    val invokeExtensionDescriptor = contextForInvoke.scopeTower.getExtensionInvokeCandidateDescriptor(expressionForInvoke)
    if (explicitReceiver != null) {
        if (invokeExtensionDescriptor == null) {
            // case 1.(foo())(), where foo() isn't extension function
            return KnownResultProcessor(emptyList())
        }
        else {
            return InvokeExtensionScopeTowerProcessor(contextForInvoke, invokeExtensionDescriptor, explicitReceiver = explicitReceiver)
        }
    }
    else {
        val usualInvoke = ExplicitReceiverScopeTowerProcessor(contextForInvoke, expressionForInvoke, ScopeTowerLevel::getFunctions) // todo operator

        if (invokeExtensionDescriptor == null) {
            return usualInvoke
        }
        else {
            return CompositeScopeTowerProcessor(
                    usualInvoke,
                    InvokeExtensionScopeTowerProcessor(contextForInvoke, invokeExtensionDescriptor, explicitReceiver = null)
            )
        }
    }

}