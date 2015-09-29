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
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetNamedDeclaration
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

public sealed class AccessTarget {
    public data class Declaration(val descriptor: VariableDescriptor): AccessTarget()
    public data class Call(val resolvedCall: ResolvedCall<*>): AccessTarget()
    public object BlackBox: AccessTarget()
}

public abstract class AccessValueInstruction protected constructor(
        element: JetElement,
        lexicalScope: LexicalScope,
        public val target: AccessTarget,
        override val receiverValues: Map<PseudoValue, ReceiverValue>
) : InstructionWithNext(element, lexicalScope), InstructionWithReceivers

public class ReadValueInstruction private constructor(
        element: JetElement,
        lexicalScope: LexicalScope,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        private var _outputValue: PseudoValue?
) : AccessValueInstruction(element, lexicalScope, target, receiverValues), InstructionWithValue {
    public constructor(
            element: JetElement,
            lexicalScope: LexicalScope,
            target: AccessTarget,
            receiverValues: Map<PseudoValue, ReceiverValue>,
            factory: PseudoValueFactory
    ): this(element, lexicalScope, target, receiverValues, null) {
        _outputValue = factory.newValue(element, this)
    }

    override val inputValues: List<PseudoValue>
        get() = receiverValues.keySet().toList()

    override val outputValue: PseudoValue
        get() = _outputValue!!

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReadValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitReadValue(this)
    }

    override fun toString(): String {
        val inVal = if (receiverValues.isEmpty()) "" else "|${receiverValues.keySet().joinToString()}"
        val targetName = when (target) {
            is AccessTarget.Declaration -> target.descriptor
            is AccessTarget.Call -> target.resolvedCall.getResultingDescriptor()
            else -> null
        }?.getName()?.asString()

        val elementText = render(element)
        val description = if (targetName != null && targetName != elementText) "$elementText, $targetName" else elementText
        return "r($description$inVal) -> $outputValue"
    }

    override fun createCopy(): InstructionImpl =
            ReadValueInstruction(element, lexicalScope, target, receiverValues, outputValue)
}

public class WriteValueInstruction(
        assignment: JetElement,
        lexicalScope: LexicalScope,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        public val TEMP_lValue: JetElement,
        public val rValue: PseudoValue
) : AccessValueInstruction(assignment, lexicalScope, target, receiverValues) {
    override val inputValues: List<PseudoValue>
        get() = (receiverValues.keySet() as Collection<PseudoValue>) + rValue

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitWriteValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitWriteValue(this)
    }

    override fun toString(): String {
        val lhs = (TEMP_lValue as? JetNamedDeclaration)?.getName() ?: render(TEMP_lValue)
        return "w($lhs|${inputValues.joinToString(", ")})"
    }

    override fun createCopy(): InstructionImpl =
            WriteValueInstruction(element, lexicalScope, target, receiverValues, TEMP_lValue, rValue)
}
