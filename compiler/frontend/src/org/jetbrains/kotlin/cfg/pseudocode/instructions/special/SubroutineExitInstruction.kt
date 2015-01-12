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

package org.jetbrains.kotlin.cfg.pseudocode.instructions.special

import org.jetbrains.kotlin.psi.JetElement
import java.util.Collections
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult

public class SubroutineExitInstruction(
        public val subroutine: JetElement,
        lexicalScope: LexicalScope,
        public val isError: Boolean
) : InstructionImpl(lexicalScope) {
    private var _sink: SubroutineSinkInstruction? = null

    public var sink: SubroutineSinkInstruction
        get() = _sink!!
        set(value: SubroutineSinkInstruction) {
            _sink = outgoingEdgeTo(value) as SubroutineSinkInstruction
        }

    override val nextInstructions: Collection<Instruction>
        get() = Collections.singleton(sink)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitSubroutineExit(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitSubroutineExit(this)
    }

    override fun toString(): String = if (isError) "<ERROR>" else "<END>"

    override fun createCopy(): InstructionImpl =
            SubroutineExitInstruction(subroutine, lexicalScope, isError)
}
