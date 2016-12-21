/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.builtins.isBuiltinExtensionFunctionalType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.calls.tasks.createSynthesizedInvokes
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

abstract class AbstractInvokeTowerProcessor<F : Candidate<FunctionDescriptor>, V : Candidate<VariableDescriptor>>(
        protected val factoryProviderForInvoke: CandidateFactoryProviderForInvoke<F, V>,
        private val variableProcessor: ScopeTowerProcessor<V>
) : ScopeTowerProcessor<F> {
    // todo optimize it
    private val previousData = ArrayList<TowerData>()
    private val invokeProcessors: MutableList<Collection<VariableInvokeProcessor>> = ArrayList()

    private inner class VariableInvokeProcessor(val variableCandidate: V): ScopeTowerProcessor<F> {
        val invokeProcessor: ScopeTowerProcessor<F> = createInvokeProcessor(variableCandidate)

        override fun process(data: TowerData)
                = invokeProcessor.process(data).map { candidateGroup ->
                    candidateGroup.map { factoryProviderForInvoke.transformCandidate(variableCandidate, it) }
                }
    }

    protected abstract fun createInvokeProcessor(variableCandidate: V): ScopeTowerProcessor<F>

    override fun process(data: TowerData): List<Collection<F>> {
        previousData.add(data)

        val candidateGroups = ArrayList<Collection<F>>(0)

        for (processorsGroup in invokeProcessors) {
            candidateGroups.addAll(processorsGroup.processVariableGroup(data))
        }

        for (variableCandidates in variableProcessor.process(data)) {
            val successfulVariables = variableCandidates.filter {
                it.isSuccessful
            }

            if (successfulVariables.isNotEmpty()) {
                val variableProcessors = successfulVariables.map { VariableInvokeProcessor(it) }
                invokeProcessors.add(variableProcessors)

                for (oldData in previousData) {
                    candidateGroups.addAll(variableProcessors.processVariableGroup(oldData))
                }
            }
        }

        return candidateGroups
    }

    private fun Collection<VariableInvokeProcessor>.processVariableGroup(data: TowerData): List<Collection<F>> {
        return when (size) {
            0 -> emptyList()
            1 -> single().process(data)
        // overload on variables see KT-10093 Resolve depends on the order of declaration for variable with implicit invoke

            else -> listOf(this.flatMap { it.process(data).flatten() })
        }
    }

}

// todo KT-9522 Allow invoke convention for synthetic property
class InvokeTowerProcessor<F : Candidate<FunctionDescriptor>, V : Candidate<VariableDescriptor>>(
        val scopeTower: ImplicitScopeTower,
        val name: Name,
        factoryProviderForInvoke: CandidateFactoryProviderForInvoke<F, V>,
        explicitReceiver: DetailedReceiver?
) : AbstractInvokeTowerProcessor<F, V>(
        factoryProviderForInvoke,
        createVariableAndObjectProcessor(scopeTower, name, factoryProviderForInvoke.factoryForVariable(stripExplicitReceiver = false), explicitReceiver)
) {

    // todo filter by operator
    override fun createInvokeProcessor(variableCandidate: V): ScopeTowerProcessor<F> {
        val (variableReceiver, invokeContext) = factoryProviderForInvoke.factoryForInvoke(variableCandidate, useExplicitReceiver = false)
                                                ?: return KnownResultProcessor(emptyList())
        return ExplicitReceiverScopeTowerProcessor(scopeTower, invokeContext, variableReceiver) { getFunctions(OperatorNameConventions.INVOKE, it) }
    }
}

class InvokeExtensionTowerProcessor<F : Candidate<FunctionDescriptor>, V : Candidate<VariableDescriptor>>(
        val scopeTower: ImplicitScopeTower,
        val name: Name,
        factoryProviderForInvoke: CandidateFactoryProviderForInvoke<F, V>,
        private val explicitReceiver: ReceiverValueWithSmartCastInfo?
) : AbstractInvokeTowerProcessor<F, V>(
        factoryProviderForInvoke,
        createVariableAndObjectProcessor(scopeTower, name, factoryProviderForInvoke.factoryForVariable(stripExplicitReceiver = true), explicitReceiver = null)
) {

    override fun createInvokeProcessor(variableCandidate: V): ScopeTowerProcessor<F> {
        val (variableReceiver, invokeContext) = factoryProviderForInvoke.factoryForInvoke(variableCandidate, useExplicitReceiver = true)
                                                ?: return KnownResultProcessor(emptyList())
        val invokeDescriptor = scopeTower.getExtensionInvokeCandidateDescriptor(variableReceiver)
                               ?: return KnownResultProcessor(emptyList())
        return InvokeExtensionScopeTowerProcessor(invokeContext, invokeDescriptor, explicitReceiver)
    }
}

private class InvokeExtensionScopeTowerProcessor<C : Candidate<FunctionDescriptor>>(
        context: CandidateFactory<FunctionDescriptor, C>,
        private val invokeCandidateDescriptor: CandidateWithBoundDispatchReceiver<FunctionDescriptor>,
        private val explicitReceiver: ReceiverValueWithSmartCastInfo?
) : AbstractSimpleScopeTowerProcessor<FunctionDescriptor, C>(context) {

    override fun simpleProcess(data: TowerData): Collection<C> {
        if (explicitReceiver != null && data == TowerData.Empty) {
            return listOf(candidateFactory.createCandidate(invokeCandidateDescriptor, ExplicitReceiverKind.BOTH_RECEIVERS, explicitReceiver))
        }

        if (explicitReceiver == null && data is TowerData.OnlyImplicitReceiver) {
            return listOf(candidateFactory.createCandidate(invokeCandidateDescriptor, ExplicitReceiverKind.DISPATCH_RECEIVER, data.implicitReceiver))
        }

        return emptyList()
    }
}

// todo debug info
private fun ImplicitScopeTower.getExtensionInvokeCandidateDescriptor(
        extensionFunctionReceiver: ReceiverValueWithSmartCastInfo
): CandidateWithBoundDispatchReceiver<FunctionDescriptor>? {
    val type = extensionFunctionReceiver.receiverValue.type
    if (!type.isBuiltinExtensionFunctionalType) return null // todo: missing smart cast?

    val invokeDescriptor = type.memberScope.getContributedFunctions(OperatorNameConventions.INVOKE, location).single()
    val synthesizedInvoke = createSynthesizedInvokes(listOf(invokeDescriptor)).single()

    // here we don't add SynthesizedDescriptor diagnostic because it should has priority as member
    return CandidateWithBoundDispatchReceiverImpl(extensionFunctionReceiver, synthesizedInvoke, listOf())
}

// case 1.(foo())() or (foo())()
fun <F : Candidate<FunctionDescriptor>> createCallTowerProcessorForExplicitInvoke(
        scopeTower: ImplicitScopeTower,
        functionContext: CandidateFactory<FunctionDescriptor, F>,
        expressionForInvoke: ReceiverValueWithSmartCastInfo,
        explicitReceiver: ReceiverValueWithSmartCastInfo?
): ScopeTowerProcessor<F> {
    val invokeExtensionDescriptor = scopeTower.getExtensionInvokeCandidateDescriptor(expressionForInvoke)
    if (explicitReceiver != null) {
        if (invokeExtensionDescriptor == null) {
            // case 1.(foo())(), where foo() isn't extension function
            return KnownResultProcessor(emptyList())
        }
        else {
            return InvokeExtensionScopeTowerProcessor(functionContext, invokeExtensionDescriptor, explicitReceiver = explicitReceiver)
        }
    }
    else {
        val usualInvoke = ExplicitReceiverScopeTowerProcessor(scopeTower, functionContext, expressionForInvoke) { getFunctions(OperatorNameConventions.INVOKE, it) } // todo operator

        if (invokeExtensionDescriptor == null) {
            return usualInvoke
        }
        else {
            return CompositeScopeTowerProcessor(
                    usualInvoke,
                    InvokeExtensionScopeTowerProcessor(functionContext, invokeExtensionDescriptor, explicitReceiver = null)
            )
        }
    }

}
