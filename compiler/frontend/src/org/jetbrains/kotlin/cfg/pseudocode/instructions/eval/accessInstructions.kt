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

package org.jetbrains.kotlin.cfg.pseudocode.instructions.eval

import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValueFactory
import org.jetbrains.kotlin.cfg.pseudocode.instructions.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

sealed class AccessTarget {
    class Declaration(val descriptor: VariableDescriptor): AccessTarget() {
        override fun equals(other: Any?) = other is Declaration && descriptor == other.descriptor

        override fun hashCode() = descriptor.hashCode()
    }
    class Call(val resolvedCall: ResolvedCall<*>): AccessTarget() {
        override fun equals(other: Any?) = other is Call && resolvedCall == other.resolvedCall

        override fun hashCode() = resolvedCall.hashCode()
    }
    object BlackBox: AccessTarget()
}

val AccessTarget.accessedDescriptor: CallableDescriptor?
    get() = when (this) {
        is AccessTarget.Declaration -> descriptor
        is AccessTarget.Call -> resolvedCall.resultingDescriptor
        is AccessTarget.BlackBox -> null
    }

abstract class AccessValueInstruction protected constructor(
        element: KtElement,
        blockScope: BlockScope,
        val target: AccessTarget,
        override val receiverValues: Map<PseudoValue, ReceiverValue>
) : InstructionWithNext(element, blockScope), InstructionWithReceivers

class ReadValueInstruction private constructor(
        element: KtElement,
        blockScope: BlockScope,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        private var _outputValue: PseudoValue?
) : AccessValueInstruction(element, blockScope, target, receiverValues), InstructionWithValue {
    constructor(
            element: KtElement,
            blockScope: BlockScope,
            target: AccessTarget,
            receiverValues: Map<PseudoValue, ReceiverValue>,
            factory: PseudoValueFactory
    ): this(element, blockScope, target, receiverValues, null) {
        _outputValue = factory.newValue(element, this)
    }

    override val inputValues: List<PseudoValue>
        get() = receiverValues.keys.toList()

    override val outputValue: PseudoValue
        get() = _outputValue!!

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReadValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitReadValue(this)
    }

    override fun toString(): String {
        val inVal = if (receiverValues.isEmpty()) "" else "|${receiverValues.keys.joinToString()}"
        val targetName = when (target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.resultingDescriptor
            else -> null
        }?.name?.asString()

        val elementText = render(element)
        val description = if (targetName != null && targetName != elementText) "$elementText, $targetName" else elementText
        return "r($description$inVal) -> $outputValue"
    }

    override fun createCopy(): InstructionImpl =
            ReadValueInstruction(element, blockScope, target, receiverValues, outputValue)
}

class WriteValueInstruction(
        assignment: KtElement,
        blockScope: BlockScope,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        val lValue: KtElement,
        val rValue: PseudoValue
) : AccessValueInstruction(assignment, blockScope, target, receiverValues) {
    override val inputValues: List<PseudoValue>
        get() = (receiverValues.keys as Collection<PseudoValue>) + rValue

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitWriteValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitWriteValue(this)
    }

    override fun toString(): String {
        val lhs = (lValue as? KtNamedDeclaration)?.name ?: render(lValue)
        return "w($lhs|${inputValues.joinToString(", ")})"
    }

    override fun createCopy(): InstructionImpl =
            WriteValueInstruction(element, blockScope, target, receiverValues, lValue, rValue)
}
