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
import org.jetbrains.jet.lang.psi.JetNamedDeclaration
import java.util.Collections
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionImpl
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue

public class WriteValueInstruction(
        assignment: JetElement,
        lexicalScope: LexicalScope,
        target: AccessTarget,
        receiverValues: Map<PseudoValue, ReceiverValue>,
        public val lValue: JetElement,
        public val rValue: PseudoValue
) : AccessValueInstruction(assignment, lexicalScope, target, receiverValues) {
    override val inputValues: List<PseudoValue>
        get() = receiverValues.keySet() + rValue

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitWriteValue(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitWriteValue(this)
    }

    override fun toString(): String {
        val lhs = (lValue as? JetNamedDeclaration)?.getName() ?: render(lValue)
        return "w($lhs|${inputValues.makeString(", ")})"
    }

    override fun createCopy(): InstructionImpl =
            WriteValueInstruction(element, lexicalScope, target, receiverValues, lValue, rValue)
}
