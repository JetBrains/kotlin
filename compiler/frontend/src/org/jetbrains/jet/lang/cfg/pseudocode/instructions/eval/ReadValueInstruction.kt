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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValueFactory
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionImpl

public class ReadValueInstruction private (
        element: JetElement,
        lexicalScope: LexicalScope,
        receiverValue: PseudoValue?,
        private var _outputValue: PseudoValue?
) : InstructionWithReceiver(element, lexicalScope, receiverValue), InstructionWithValue {
    private fun newResultValue(factory: PseudoValueFactory) {
        _outputValue = factory.newValue(element, this)
    }

    override val outputValue: PseudoValue
        get() = _outputValue!!

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitReadValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitReadValue(this)
    }

    override fun toString(): String {
        val inVal = if (receiverValue != null) "|$receiverValue" else ""
        return "r(${render(element)}$inVal) -> $outputValue"
    }

    override fun createCopy(): InstructionImpl =
            ReadValueInstruction(element, lexicalScope, receiverValue, outputValue)

    class object {
        public fun create (
                element: JetElement,
                lexicalScope: LexicalScope,
                receiverValue: PseudoValue?,
                factory: PseudoValueFactory
        ): ReadValueInstruction {
            return ReadValueInstruction(element, lexicalScope, receiverValue, null).let { instruction ->
                instruction.newResultValue(factory)
                instruction
            }
        }
    }
}
