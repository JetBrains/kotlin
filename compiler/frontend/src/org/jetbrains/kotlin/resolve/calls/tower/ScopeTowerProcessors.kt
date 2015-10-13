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

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.calls.tasks.ExplicitReceiverKind
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassQualifier
import org.jetbrains.kotlin.resolve.scopes.receivers.QualifierReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.Receiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.utils.addToStdlib.check


internal class KnownResultProcessor<C>(
        result: Collection<C>
): ScopeTowerProcessor<C> {
    var candidates = result

    override fun processTowerLevel(level: ScopeTowerLevel) {
        candidates = emptyList()
    }

    override fun processImplicitReceiver(implicitReceiver: ReceiverValue) {
        candidates = emptyList()
    }

    override fun getCandidatesGroups() = listOfNotNull(candidates.check { it.isNotEmpty() })
}

internal class CompositeScopeTowerProcessor<C>(
        vararg val processors: ScopeTowerProcessor<C>
) : ScopeTowerProcessor<C> {
    override fun processTowerLevel(level: ScopeTowerLevel) = processors.forEach { it.processTowerLevel(level) }

    override fun processImplicitReceiver(implicitReceiver: ReceiverValue)
            = processors.forEach { it.processImplicitReceiver(implicitReceiver) }

    override fun getCandidatesGroups(): List<Collection<C>> = processors.flatMap { it.getCandidatesGroups() }
}

internal abstract class AbstractScopeTowerProcessor<C>(
        val context: TowerContext<C>
) : ScopeTowerProcessor<C> {
    protected val name: Name get() = context.name

    protected abstract var candidates: Collection<C>

    override fun getCandidatesGroups() = listOfNotNull(candidates.check { it.isNotEmpty() })
}

internal class ExplicitReceiverScopeTowerProcessor<C>(
        context: TowerContext<C>,
        val explicitReceiver: ReceiverValue,
        val collectCandidates: ScopeTowerLevel.(Name) -> Collection<CandidateWithBoundDispatchReceiver<*>>
): AbstractScopeTowerProcessor<C>(context) {
    override var candidates = resolveAsMember()

    override fun processTowerLevel(level: ScopeTowerLevel) {
        candidates = resolveAsExtension(level)
    }

    override fun processImplicitReceiver(implicitReceiver: ReceiverValue) {
        // no candidates, because we already have receiver
        candidates = emptyList()
    }

    private fun resolveAsMember(): Collection<C> {
        val members = ReceiverScopeTowerLevel(context.scopeTower, explicitReceiver).collectCandidates(name).filter { !it.requiresExtensionReceiver }
        return members.map { context.createCandidate(it, ExplicitReceiverKind.DISPATCH_RECEIVER, extensionReceiver = null) }
    }

    private fun resolveAsExtension(level: ScopeTowerLevel): Collection<C> {
        val extensions = level.collectCandidates(name).filter { it.requiresExtensionReceiver }
        return extensions.map { context.createCandidate(it, ExplicitReceiverKind.EXTENSION_RECEIVER, extensionReceiver = explicitReceiver) }
    }
}

private class QualifierScopeTowerProcessor<C>(
        context: TowerContext<C>,
        val qualifier: QualifierReceiver,
        val collectCandidates: ScopeTowerLevel.(Name) -> Collection<CandidateWithBoundDispatchReceiver<*>>
): AbstractScopeTowerProcessor<C>(context) {
    override var candidates = resolve()

    override fun processTowerLevel(level: ScopeTowerLevel) {
        // no candidates, because we done all already
        candidates = emptyList()
    }

    override fun processImplicitReceiver(implicitReceiver: ReceiverValue) {
        // no candidates, because we done all already
        candidates = emptyList()
    }

    private fun resolve(): Collection<C> {
        val staticMembers = QualifierScopeTowerLevel(context.scopeTower, qualifier).collectCandidates(name)
                .filter { !it.requiresExtensionReceiver }
                .map { context.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = null) }
        return staticMembers
    }
}

private class NoExplicitReceiverScopeTowerProcessor<C>(
        context: TowerContext<C>,
        val collectCandidates: ScopeTowerLevel.(Name) -> Collection<CandidateWithBoundDispatchReceiver<*>>
) : AbstractScopeTowerProcessor<C>(context) {
    override var candidates: Collection<C> = emptyList()

    private var descriptorsRequestImplicitReceiver = emptyList<CandidateWithBoundDispatchReceiver<*>>()

    override fun processTowerLevel(level: ScopeTowerLevel) {
        val descriptors = level.collectCandidates(name)

        descriptorsRequestImplicitReceiver = descriptors.filter { it.requiresExtensionReceiver }

        candidates = descriptors.filter { !it.requiresExtensionReceiver }
                .map { context.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = null) }
    }

    override fun processImplicitReceiver(implicitReceiver: ReceiverValue) {
        candidates = descriptorsRequestImplicitReceiver
                .map { context.createCandidate(it, ExplicitReceiverKind.NO_EXPLICIT_RECEIVER, extensionReceiver = implicitReceiver) }
    }
}

private fun <C> createSimpleProcessor(
        context: TowerContext<C>,
        explicitReceiver: Receiver?,
        collectCandidates: ScopeTowerLevel.(Name) -> Collection<CandidateWithBoundDispatchReceiver<*>>
) : ScopeTowerProcessor<C> {
    if (explicitReceiver is ReceiverValue) {
        return ExplicitReceiverScopeTowerProcessor(context, explicitReceiver, collectCandidates)
    }
    else if (explicitReceiver is QualifierReceiver) {
        val qualifierProcessor = QualifierScopeTowerProcessor(context, explicitReceiver, collectCandidates)

        // todo enum entry, object.
        val classValue = (explicitReceiver as? ClassQualifier)?.classValueReceiver ?: return qualifierProcessor
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

internal fun <C> createVariableProcessor(context: TowerContext<C>, explicitReceiver: Receiver?)
        = createSimpleProcessor(context, explicitReceiver, ScopeTowerLevel::getVariables)

internal fun <C> createFunctionProcessor(context: TowerContext<C>, explicitReceiver: Receiver?)
        = createSimpleProcessor(context, explicitReceiver, ScopeTowerLevel::getFunctions)
