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

import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValueFactory
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.cfg.pseudocode.TypePredicate

public abstract class OperationInstruction protected(
        element: JetElement,
        lexicalScope: LexicalScope,
        override val inputValues: List<PseudoValue>
) : InstructionWithNext(element, lexicalScope), InstructionWithValue {
    protected var resultValue: PseudoValue? = null

    override val outputValue: PseudoValue?
        get() = resultValue

    protected fun renderInstruction(name: String, desc: String): String =
            "$name($desc" +
            (if (inputValues.isNotEmpty()) "|${inputValues.joinToString(", ")})" else ")") +
            (if (resultValue != null) " -> $resultValue" else "")

    protected fun setResult(value: PseudoValue?): OperationInstruction {
        this.resultValue = value
        return this
    }

    protected fun setResult(factory: PseudoValueFactory?, valueElement: JetElement? = element): OperationInstruction {
        return setResult(factory?.newValue(valueElement, this))
    }
}

trait StrictlyValuedOperationInstruction: OperationInstruction {
    override val outputValue: PseudoValue
        get() = resultValue!!
}

public class CallInstruction private(
        element: JetElement,
        lexicalScope: LexicalScope,
        val resolvedCall: ResolvedCall<*>,
        override val receiverValues: Map<PseudoValue, ReceiverValue>,
        public val arguments: Map<PseudoValue, ValueParameterDescriptor>
) : OperationInstruction(element, lexicalScope, receiverValues.keySet() + arguments.keySet()), InstructionWithReceivers {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitCallInstruction(this)
    }

    override fun createCopy() =
            CallInstruction(element, lexicalScope, resolvedCall, receiverValues, arguments).setResult(resultValue)

    override fun toString() =
            renderInstruction("call", "${render(element)}, ${resolvedCall.getResultingDescriptor()!!.getName()}")

    default object Factory {
        fun create (
                element: JetElement,
                lexicalScope: LexicalScope,
                resolvedCall: ResolvedCall<*>,
                receiverValues: Map<PseudoValue, ReceiverValue>,
                arguments: Map<PseudoValue, ValueParameterDescriptor>,
                factory: PseudoValueFactory?
        ): CallInstruction =
                CallInstruction(element, lexicalScope, resolvedCall, receiverValues, arguments).setResult(factory, element) as CallInstruction
    }
}

// Introduces black-box operation
// Used to:
//      consume input values (so that they aren't considered unused)
//      denote value transformation which can't be expressed by other instructions (such as call or read)
//      pass more than one value to instruction which formally requires only one (e.g. jump)
public class MagicInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        inputValues: List<PseudoValue>,
        val expectedTypes: Map<PseudoValue, TypePredicate>,
        val kind: MagicKind
) : OperationInstruction(element, lexicalScope, inputValues), StrictlyValuedOperationInstruction {
    public val synthetic: Boolean get() = outputValue.element == null

    override fun accept(visitor: InstructionVisitor) = visitor.visitMagic(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMagic(this)

    override fun createCopy() =
            MagicInstruction(element, lexicalScope, inputValues, expectedTypes, kind).setResult(resultValue)

    override fun toString() = renderInstruction("magic[$kind]", render(element))

    default object Factory {
        fun create(
                element: JetElement,
                valueElement: JetElement?,
                lexicalScope: LexicalScope,
                inputValues: List<PseudoValue>,
                expectedTypes: Map<PseudoValue, TypePredicate>,
                kind: MagicKind,
                factory: PseudoValueFactory
        ): MagicInstruction = MagicInstruction(
                element, lexicalScope, inputValues, expectedTypes, kind
        ).setResult(factory, valueElement) as MagicInstruction
    }
}

public enum class MagicKind(val sideEffectFree: Boolean = false) {
    // builtin operations
    STRING_TEMPLATE: MagicKind(true)
    AND: MagicKind(true)
    OR: MagicKind(true)
    NOT_NULL_ASSERTION: MagicKind()
    EQUALS_IN_WHEN_CONDITION: MagicKind()
    IS: MagicKind()
    CAST: MagicKind()
    CALLABLE_REFERENCE: MagicKind(true)
    // implicit operations
    LOOP_RANGE_ITERATION: MagicKind()
    IMPLICIT_RECEIVER: MagicKind()
    VALUE_CONSUMER: MagicKind()
    // unrecognized operations
    UNRESOLVED_CALL: MagicKind()
    UNSUPPORTED_ELEMENT: MagicKind()
    UNRECOGNIZED_WRITE_RHS: MagicKind()
    FAKE_INITIALIZER: MagicKind()
}

// Merges values produced by alternative control-flow paths (such as 'if' branches)
class MergeInstruction private(
        element: JetElement,
        lexicalScope: LexicalScope,
        inputValues: List<PseudoValue>
): OperationInstruction(element, lexicalScope, inputValues), StrictlyValuedOperationInstruction {
    override fun accept(visitor: InstructionVisitor) = visitor.visitMerge(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMerge(this)

    override fun createCopy() = MergeInstruction(element, lexicalScope, inputValues).setResult(resultValue)

    override fun toString() = renderInstruction("merge", render(element))

    default object Factory {
        fun create(
                element: JetElement,
                lexicalScope: LexicalScope,
                inputValues: List<PseudoValue>,
                factory: PseudoValueFactory
        ): MergeInstruction = MergeInstruction(element, lexicalScope, inputValues).setResult(factory) as MergeInstruction
    }
}
