/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps

import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.cfg.Label
import java.util.Arrays
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.Instruction
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.jet.utils.emptyOrSingletonList

public class ConditionalJumpInstruction(
        element: JetElement,
        public val onTrue: Boolean,
        lexicalScope: LexicalScope,
        targetLabel: Label,
        public val conditionValue: PseudoValue?) : AbstractJumpInstruction(element, targetLabel, lexicalScope) {
    private var _nextOnTrue: Instruction? = null
    private var _nextOnFalse: Instruction? = null

    public var nextOnTrue: Instruction
        get() = _nextOnTrue!!
        set(value: Instruction) {
            _nextOnTrue = outgoingEdgeTo(value)
        }

    public var nextOnFalse: Instruction
        get() = _nextOnFalse!!
        set(value: Instruction) {
            _nextOnFalse = outgoingEdgeTo(value)
        }

    override val nextInstructions: Collection<Instruction>
        get() = Arrays.asList(nextOnFalse, nextOnTrue)

    override val inputValues: List<PseudoValue>
        get() = emptyOrSingletonList(conditionValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitConditionalJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitConditionalJump(this)
    }

    override fun toString(): String {
        val instr = if (onTrue) "jt" else "jf"
        val inValue = conditionValue?.let { "|" + it } ?: ""
        return "$instr(${targetLabel.getName()}$inValue)"
    }

    override fun createCopy(newLabel: Label, lexicalScope: LexicalScope): AbstractJumpInstruction =
            ConditionalJumpInstruction(element, onTrue, lexicalScope, newLabel, conditionValue)
}