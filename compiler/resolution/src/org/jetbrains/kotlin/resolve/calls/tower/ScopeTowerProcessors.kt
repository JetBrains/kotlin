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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.DetailedReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo


class KnownResultProcessor<out C>(
        val result: Collection<C>
): ScopeTowerProcessor<C> {
    override fun process(data: TowerData)
            = if (data == TowerData.Empty) listOfNotNull(result.takeIf { it.isNotEmpty() }) else emptyList()
}

class CompositeScopeTowerProcessor<out C>(
        vararg val processors: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    override fun process(data: TowerData): List<Collection<C>> = processors.flatMap { it.process(data) }
}

internal abstract class AbstractSimpleScopeTowerProcessor<C: Candidate>(
        val candidateFactory: CandidateFactory<C>
) : ScopeTowerProcessor<C> {

    protected abstract fun simpleProcess(data: TowerData): Collection<C>

    override fun process(data: TowerData): List<Collection<C>> = listOfNotNull(simpleProcess(data).takeIf { it.isNotEmpty() })
}

private typealias CandidatesCollector =
    ScopeTowerLevel.(extensionReceiver: ReceiverValueWithSmartCastInfo?) -> Collection<CandidateWithBoundDispatchReceiver>

internal class ExplicitReceiverScopeTowerProcessor<C: Candidate>(
        val scopeTower: ImplicitScopeTower,
        context: CandidateFactory<C>,
        val explicitReceiver: ReceiverValueWithSmartCastInfo,
        val collectCandidates: CandidatesCollector
): AbstractSimpleScopeTowerProcessor<C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> {
        return when (data) {
            TowerData.Empty -> resolveAsMember()
            is TowerData.TowerLevel -> resolveAsExtension(data.level)
            else -> emptyList()
        }
    }

    private fun resolveAsMember(): Collection<C> {
        val members = mutableListOf<C>()
        for (memberCandidate in MemberScopeTowerLevel(scopeTower, explicitReceiver).collectCandidates(null)) {
            if (!memberCandidate.requiresExtensionReceiver) {
                members.add(candidateFactory.createCandidate(memberCandidate, ExplicitReceiverKind.DISPATCH_RECEIVER, extensionReceiver = null))
            }
        }
        return members
    }

    private fun resolveAsExtension(level: ScopeTowerLevel): Collection<C> {
        val extensions = mutableListOf<C>()
        for (extensionCandidate in level.collectCandidates(explicitReceiver)) {
            if (extensionCandidate.requiresExtensionReceiver) {
                extensions.add(candidateFactory.createCandidate(extensionCandidate, ExplicitReceiverKind.EXTENSION_RECEIVER, extensionReceiver = explicitReceiver))
            }
        }
        return extensions
    }
}

private class QualifierScopeTowerProcessor<C: Candidate>(
        val scopeTower: ImplicitScopeTower,
        context: CandidateFactory<C>,
        val qualifier: QualifierReceiver,
        val collectCandidates: CandidatesCollector
): AbstractSimpleScopeTowerProcessor<C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> {
        if (data != TowerData.Empty) return emptyList()

        val staticMembers = mutableListOf<C>()
        for (towerCandidate in QualifierScopeTowerLevel(scopeTower, qualifier).collectCandidates(null)) {
            if (!towerCandidate.requiresExtensionReceiver) {
                staticMembers.add(candidateFactory.createCandidate(towerCandidate, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = null))
            }
        }
        return staticMembers
    }
}

private class NoExplicitReceiverScopeTowerProcessor<C: Candidate>(
        context: CandidateFactory<C>,
        val collectCandidates: CandidatesCollector
) : AbstractSimpleScopeTowerProcessor<C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C>
            = when(data) {
                is TowerData.TowerLevel -> {
                    val result = mutableListOf<C>()
                    for (towerCandidate in data.level.collectCandidates(null)) {
                        if (!towerCandidate.requiresExtensionReceiver) {
                            result.add(candidateFactory.createCandidate(towerCandidate, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = null))
                        }
                    }
                    result
                }
                is TowerData.BothTowerLevelAndImplicitReceiver -> {
                    val result = mutableListOf<C>()
                    for (towerCandidate in data.level.collectCandidates(data.implicitReceiver)) {
                        if (towerCandidate.requiresExtensionReceiver) {
                            result.add(candidateFactory.createCandidate(towerCandidate, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = data.implicitReceiver))
                        }
                    }
                    result
                }
                else -> emptyList()
            }

}

private fun <D : CallableDescriptor, C : Candidate> processCommonAndSyntheticMembers(
        receiverForMember: ReceiverValueWithSmartCastInfo,
        scopeTowerLevel: ScopeTowerLevel,
        collectCandidates: CandidatesCollector,
        candidateFactory: CandidateFactory<C>,
        isExplicitReceiver: Boolean
): List<C> {
    val (members, syntheticExtension) =
            scopeTowerLevel.collectCandidates(null)
                    .filter {
                        it.descriptor.dispatchReceiverParameter == null || it.descriptor.extensionReceiverParameter == null
                    }.partition { !it.requiresExtensionReceiver }

    return members.map {
               candidateFactory.createCandidate(
                       it,
                       if (isExplicitReceiver) ExplicitReceiverKind.DISPATCH_RECEIVER else ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                       extensionReceiver = null
               )
           } +
           syntheticExtension.map {
               candidateFactory.createCandidate(
                       it,
                       if (isExplicitReceiver) ExplicitReceiverKind.EXTENSION_RECEIVER else ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                       extensionReceiver = receiverForMember
               )
           }
}

private fun <C : Candidate> createSimpleProcessor(
        scopeTower: ImplicitScopeTower,
        context: CandidateFactory<C>,
        explicitReceiver: DetailedReceiver?,
        classValueReceiver: Boolean,
        collectCandidates: CandidatesCollector
) : ScopeTowerProcessor<C> {
    return when (explicitReceiver) {
        is ReceiverValueWithSmartCastInfo -> {
            ExplicitReceiverScopeTowerProcessor(scopeTower, context, explicitReceiver, collectCandidates)
        }
        is QualifierReceiver -> {
            val qualifierProcessor = QualifierScopeTowerProcessor(scopeTower, context, explicitReceiver, collectCandidates)
            if (!classValueReceiver) return qualifierProcessor

            // todo enum entry, object.
            val classValue = explicitReceiver.classValueReceiverWithSmartCastInfo ?: return qualifierProcessor
            CompositeScopeTowerProcessor(
                    qualifierProcessor,
                    ExplicitReceiverScopeTowerProcessor(scopeTower, context, classValue, collectCandidates)
            )
        }
        else -> {
            assert(explicitReceiver == null) {
                "Illegal explicit receiver: $explicitReceiver(${explicitReceiver!!::class.java.simpleName})"
            }
            NoExplicitReceiverScopeTowerProcessor(context, collectCandidates)
        }
    }
}

fun <C : Candidate> createVariableProcessor(scopeTower: ImplicitScopeTower, name: Name,
                                            context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getVariables(name, it) }

fun <C : Candidate> createVariableAndObjectProcessor(scopeTower: ImplicitScopeTower, name: Name,
                                                     context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = CompositeScopeTowerProcessor(
        createVariableProcessor(scopeTower, name, context, explicitReceiver),
        createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getObjects(name, it) }
)

fun <C : Candidate> createSimpleFunctionProcessor(scopeTower: ImplicitScopeTower, name: Name,
                                                  context: CandidateFactory<C>, explicitReceiver: DetailedReceiver?, classValueReceiver: Boolean = true
) = createSimpleProcessor(scopeTower, context, explicitReceiver, classValueReceiver) { getFunctions(name, it) }


fun <С: Candidate> createFunctionProcessor(
        scopeTower: ImplicitScopeTower,
        name: Name,
        simpleContext: CandidateFactory<С>,
        factoryProviderForInvoke: CandidateFactoryProviderForInvoke<С>,
        explicitReceiver: DetailedReceiver?
): CompositeScopeTowerProcessor<С> {

    // a.foo() -- simple function call
    val simpleFunction = createSimpleFunctionProcessor(scopeTower, name, simpleContext, explicitReceiver)

    // a.foo() -- property a.foo + foo.invoke()
    val invokeProcessor = InvokeTowerProcessor(scopeTower, name, factoryProviderForInvoke, explicitReceiver)

    // a.foo() -- property foo is extension function with receiver a -- a.invoke()
    val invokeExtensionProcessor = createProcessorWithReceiverValueOrEmpty(explicitReceiver) {
        InvokeExtensionTowerProcessor(scopeTower, name, factoryProviderForInvoke, it)
    }

    return CompositeScopeTowerProcessor(simpleFunction, invokeProcessor, invokeExtensionProcessor)
}


fun <C: Candidate> createProcessorWithReceiverValueOrEmpty(
        explicitReceiver: DetailedReceiver?,
        create: (ReceiverValueWithSmartCastInfo?) -> ScopeTowerProcessor<C>
): ScopeTowerProcessor<C> {
    return if (explicitReceiver is QualifierReceiver) {
        explicitReceiver.classValueReceiverWithSmartCastInfo?.let(create)
        ?: KnownResultProcessor<C>(listOf())
    }
    else {
        create(explicitReceiver as ReceiverValueWithSmartCastInfo?)
    }
}
