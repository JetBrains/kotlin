/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import kotlin.properties.Delegates

abstract class OperationInstruction protected(
        element: JetElement,
        lexicalScope: LexicalScope,
        val usedValues: List<PseudoValue>
) : InstructionWithNext(element, lexicalScope) {
    protected var resultValue: PseudoValue? = null

    override fun getInputValues(): List<PseudoValue> = usedValues
    override fun getOutputValue(): PseudoValue? = resultValue

    protected fun renderInstruction(name: String, desc: String): String =
            "$name($desc" +
            (if (usedValues.notEmpty) "|${usedValues.makeString(", ")})" else ")") +
            (if (resultValue != null) " -> $resultValue" else "")

    protected fun setResult(value: PseudoValue?): OperationInstruction {
        this.resultValue = value
        return this
    }

    protected fun setResult(factory: PseudoValueFactory?, valueElement: JetElement? = element): OperationInstruction {
        return setResult(factory?.newValue(valueElement, this))
    }
}

class CallInstruction private(
        element: JetElement,
        lexicalScope: LexicalScope,
        val resolvedCall: ResolvedCall<*>,
        usedValues: List<PseudoValue>
) : OperationInstruction(element, lexicalScope, usedValues) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitCallInstruction(this)
    }

    override fun createCopy() =
            CallInstruction(element, lexicalScope, resolvedCall, usedValues).setResult(resultValue)

    override fun toString() =
            renderInstruction("call", "${render(element)}, ${resolvedCall.getResultingDescriptor()!!.getName()}")

    class object {
        fun create (
                element: JetElement,
                lexicalScope: LexicalScope,
                resolvedCall: ResolvedCall<*>,
                usedValues: List<PseudoValue>,
                factory: PseudoValueFactory?
        ): CallInstruction = CallInstruction(element, lexicalScope, resolvedCall, usedValues).setResult(factory) as CallInstruction
    }
}

// Introduces black-box operation
// Used to:
//      consume input values (so that they aren't considered unused)
//      denote value transformation which can't be expressed by other instructions (such as call or read)
//      pass more than one value to instruction which formally requires only one (e.g. jump)
// "Synthetic" means that the instruction does not correspond to some operation explicitly expressed by PSI element
//      Examples: merging branches of 'if', 'when' and 'try' expressions, providing initial values for parameters, etc.
class MagicInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val synthetic: Boolean,
        usedValues: List<PseudoValue>
) : OperationInstruction(element, lexicalScope, usedValues) {
    override fun getOutputValue(): PseudoValue = resultValue!!

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitMagic(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitMagic(this)
    }

    override fun createCopy() =
            MagicInstruction(element, lexicalScope, synthetic, usedValues).setResult(resultValue)

    override fun toString() = renderInstruction("magic", render(element))

    class object {
        fun create(
                element: JetElement,
                valueElement: JetElement?,
                lexicalScope: LexicalScope,
                synthetic: Boolean,
                usedValues: List<PseudoValue>,
                factory: PseudoValueFactory
        ): MagicInstruction = MagicInstruction(element, lexicalScope, synthetic, usedValues).setResult(factory, valueElement) as MagicInstruction
    }
}

class CompilationErrorInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val message: String
) : InstructionWithNext(element, lexicalScope) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCompilationErrorInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitCompilationErrorInstruction(this)
    }

    override fun createCopy() = CompilationErrorInstruction(element, lexicalScope, message)

    override fun toString() = "error(${render(element)}, $message)"
}

// This instruciton is used to let the dead code detector know the syntactic structure of unreachable code
// otherwise only individual parts of expression would be reported as unreachable
// e.g. for (i in foo) {} -- only i and foo would be marked unreachable
class MarkInstruction(
        element: JetElement,
        lexicalScope: LexicalScope
) : InstructionWithNext(element, lexicalScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitMarkInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitMarkInstruction(this)
    }

    override fun createCopy() = MarkInstruction(element, lexicalScope)

    override fun toString() = "mark(${render(element)})"
}