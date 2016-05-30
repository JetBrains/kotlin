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
import org.jetbrains.kotlin.cfg.pseudocode.TypePredicate
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

abstract class OperationInstruction protected constructor(
        element: KtElement,
        blockScope: BlockScope,
        override val inputValues: List<PseudoValue>
) : InstructionWithNext(element, blockScope), InstructionWithValue {
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

    protected fun setResult(factory: PseudoValueFactory?, valueElement: KtElement? = element): OperationInstruction {
        return setResult(factory?.newValue(valueElement, this))
    }
}

class CallInstruction private constructor(
        element: KtElement,
        blockScope: BlockScope,
        val resolvedCall: ResolvedCall<*>,
        override val receiverValues: Map<PseudoValue, ReceiverValue>,
        val arguments: Map<PseudoValue, ValueParameterDescriptor>
) : OperationInstruction(element, blockScope, (receiverValues.keys as Collection<PseudoValue>) + arguments.keys), InstructionWithReceivers {

    constructor (
            element: KtElement,
            blockScope: BlockScope,
            resolvedCall: ResolvedCall<*>,
            receiverValues: Map<PseudoValue, ReceiverValue>,
            arguments: Map<PseudoValue, ValueParameterDescriptor>,
            factory: PseudoValueFactory?
    ): this(element, blockScope, resolvedCall, receiverValues, arguments) {
        setResult(factory)
    }

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitCallInstruction(this)
    }

    override fun createCopy() =
            CallInstruction(element, blockScope, resolvedCall, receiverValues, arguments).setResult(resultValue)

    override fun toString() =
            renderInstruction("call", "${render(element)}, ${resolvedCall.resultingDescriptor!!.name}")
}

// Introduces black-box operation
// Used to:
//      consume input values (so that they aren't considered unused)
//      denote value transformation which can't be expressed by other instructions (such as call or read)
//      pass more than one value to instruction which formally requires only one (e.g. jump)
class MagicInstruction(
        element: KtElement,
        blockScope: BlockScope,
        inputValues: List<PseudoValue>,
        val kind: MagicKind
) : OperationInstruction(element, blockScope, inputValues) {
    constructor (
            element: KtElement,
            valueElement: KtElement?,
            blockScope: BlockScope,
            inputValues: List<PseudoValue>,
            kind: MagicKind,
            factory: PseudoValueFactory
    ): this(element, blockScope, inputValues, kind) {
        setResult(factory, valueElement)
    }

    val synthetic: Boolean get() = outputValue.element == null

    override val outputValue: PseudoValue
        get() = resultValue!!

    override fun accept(visitor: InstructionVisitor) = visitor.visitMagic(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMagic(this)

    override fun createCopy() =
            MagicInstruction(element, blockScope, inputValues, kind).setResult(resultValue)

    override fun toString() = renderInstruction("magic[$kind]", render(element))
}

enum class MagicKind(val sideEffectFree: Boolean = false) {
    // builtin operations
    STRING_TEMPLATE(true),
    AND(true),
    OR(true),
    NOT_NULL_ASSERTION(),
    EQUALS_IN_WHEN_CONDITION(),
    IS(),
    CAST(),
    UNBOUND_CALLABLE_REFERENCE(true),
    BOUND_CALLABLE_REFERENCE(),
    // implicit operations
    LOOP_RANGE_ITERATION(),
    IMPLICIT_RECEIVER(),
    VALUE_CONSUMER(),
    // unrecognized operations
    UNRESOLVED_CALL(),
    UNSUPPORTED_ELEMENT(),
    UNRECOGNIZED_WRITE_RHS(),
    FAKE_INITIALIZER(),
    EXHAUSTIVE_WHEN_ELSE()
}

// Merges values produced by alternative control-flow paths (such as 'if' branches)
class MergeInstruction private constructor(
        element: KtElement,
        blockScope: BlockScope,
        inputValues: List<PseudoValue>
): OperationInstruction(element, blockScope, inputValues) {
    constructor (
            element: KtElement,
            blockScope: BlockScope,
            inputValues: List<PseudoValue>,
            factory: PseudoValueFactory
    ): this(element, blockScope, inputValues) {
        setResult(factory)
    }

    override val outputValue: PseudoValue
        get() = resultValue!!

    override fun accept(visitor: InstructionVisitor) = visitor.visitMerge(this)

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitMerge(this)

    override fun createCopy() = MergeInstruction(element, blockScope, inputValues).setResult(resultValue)

    override fun toString() = renderInstruction("merge", render(element))
}
