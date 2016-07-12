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
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.coroutine.CoroutineReceiverValue
import org.jetbrains.kotlin.resolve.coroutine.createCoroutineSuspensionFunctionView
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.utils.addToStdlib.check


class KnownResultProcessor<out C>(
        val result: Collection<C>
): ScopeTowerProcessor<C> {
    override fun process(data: TowerData)
            = if (data == TowerData.Empty) listOfNotNull(result.check { it.isNotEmpty() }) else emptyList()
}

class CompositeScopeTowerProcessor<out C>(
        vararg val processors: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    override fun process(data: TowerData): List<Collection<C>> = processors.flatMap { it.process(data) }
}

internal abstract class AbstractSimpleScopeTowerProcessor<D : CallableDescriptor, C: Candidate<D>>(
        val context: TowerContext<D, C>
) : ScopeTowerProcessor<C> {
    protected val name: Name get() = context.name

    protected abstract fun simpleProcess(data: TowerData): Collection<C>

    override fun process(data: TowerData): List<Collection<C>> = listOfNotNull(simpleProcess(data).check { it.isNotEmpty() })
}

internal class ExplicitReceiverScopeTowerProcessor<D : CallableDescriptor, C: Candidate<D>>(
        context: TowerContext<D, C>,
        val explicitReceiver: ReceiverValue,
        val collectCandidates: ScopeTowerLevel.(name: Name, extensionReceiver: ReceiverValue?) -> Collection<CandidateWithBoundDispatchReceiver<D>>
): AbstractSimpleScopeTowerProcessor<D, C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> {
        return when (data) {
            TowerData.Empty -> resolveAsMember()
            is TowerData.TowerLevel -> resolveAsExtension(data.level)
            else -> emptyList()
        }
    }

    private fun resolveAsMember(): Collection<C> {
        val members = ReceiverScopeTowerLevel(context.scopeTower, explicitReceiver)
                .collectCandidates(name, null).filter { !it.requiresExtensionReceiver }
        return members.map { context.createCandidate(it, ExplicitReceiverKind.DISPATCH_RECEIVER, extensionReceiver = null) }
    }

    private fun resolveAsExtension(level: ScopeTowerLevel): Collection<C> {
        val extensions = level.collectCandidates(name, explicitReceiver).filter { it.requiresExtensionReceiver }
        return extensions.map { context.createCandidate(it, ExplicitReceiverKind.EXTENSION_RECEIVER, extensionReceiver = explicitReceiver) }
    }
}

private class QualifierScopeTowerProcessor<D : CallableDescriptor, C: Candidate<D>>(
        context: TowerContext<D, C>,
        val qualifier: QualifierReceiver,
        val collectCandidates: ScopeTowerLevel.(name: Name, extensionReceiver: ReceiverValue?) -> Collection<CandidateWithBoundDispatchReceiver<D>>
): AbstractSimpleScopeTowerProcessor<D, C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C> {
        if (data != TowerData.Empty) return emptyList()

        val staticMembers = QualifierScopeTowerLevel(context.scopeTower, qualifier).collectCandidates(name, null)
                .filter { !it.requiresExtensionReceiver }
                .map { context.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = null) }
        return staticMembers
    }
}

private class NoExplicitReceiverScopeTowerProcessor<D : CallableDescriptor, C: Candidate<D>>(
        context: TowerContext<D, C>,
        val collectCandidates: ScopeTowerLevel.(name: Name, extensionReceiver: ReceiverValue?) -> Collection<CandidateWithBoundDispatchReceiver<D>>
) : AbstractSimpleScopeTowerProcessor<D, C>(context) {
    override fun simpleProcess(data: TowerData): Collection<C>
            = when(data) {
                is TowerData.TowerLevel -> {
                    data.level.collectCandidates(name, null).filter { !it.requiresExtensionReceiver }.map {
                        context.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = null)
                    }
                }
                is TowerData.BothTowerLevelAndImplicitReceiver -> {
                    val result = mutableListOf<C>()

                    data.level.collectCandidates(name, data.implicitReceiver).filter { it.requiresExtensionReceiver }.forEach {
                        result.add(
                            context.createCandidate(
                                    it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = data.implicitReceiver))

                        if (data.implicitReceiver is CoroutineReceiverValue) {
                            val newDescriptor = it.descriptor.createCoroutineSuspensionFunctionView() ?: return@forEach
                            result.add(
                                context.createCandidate(
                                        it.copy(newDescriptor = newDescriptor),
                                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = data.implicitReceiver))
                        }
                    }

                    result
                }
                else -> emptyList()
            }
}

private fun <D : CallableDescriptor, C: Candidate<D>> createSimpleProcessor(
        context: TowerContext<D, C>,
        explicitReceiver: Receiver?,
        collectCandidates: ScopeTowerLevel.(name: Name, extensionReceiver: ReceiverValue?) -> Collection<CandidateWithBoundDispatchReceiver<D>>
) : ScopeTowerProcessor<C> {
    if (explicitReceiver is ReceiverValue) {
        return ExplicitReceiverScopeTowerProcessor(context, explicitReceiver, collectCandidates)
    }
    else if (explicitReceiver is QualifierReceiver) {
        val qualifierProcessor = QualifierScopeTowerProcessor(context, explicitReceiver, collectCandidates)

        // todo enum entry, object.
        val classValue = explicitReceiver.classValueReceiver ?: return qualifierProcessor
        return CompositeScopeTowerProcessor(
                qualifierProcessor,
                ExplicitReceiverScopeTowerProcessor(context, classValue, collectCandidates)
        )
    }
    else {
        assert(explicitReceiver == null) {
            "Illegal explicit receiver: $explicitReceiver(${explicitReceiver!!.javaClass.simpleName})"
        }
        return NoExplicitReceiverScopeTowerProcessor(context, collectCandidates)
    }
}

fun <C : Candidate<VariableDescriptor>> createVariableProcessor(context: TowerContext<VariableDescriptor, C>, explicitReceiver: Receiver?)
        = createSimpleProcessor(context, explicitReceiver, ScopeTowerLevel::getVariables)

fun <C : Candidate<VariableDescriptor>> createVariableAndObjectProcessor(context: TowerContext<VariableDescriptor, C>, explicitReceiver: Receiver?) =
        CompositeScopeTowerProcessor(
                createVariableProcessor(context, explicitReceiver),
                createSimpleProcessor(context, explicitReceiver, ScopeTowerLevel::getObjects)
        )

fun <C : Candidate<FunctionDescriptor>> createFunctionProcessor(context: TowerContext<FunctionDescriptor, C>, explicitReceiver: Receiver?)
        = createSimpleProcessor(context, explicitReceiver, ScopeTowerLevel::getFunctions)
