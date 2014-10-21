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

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.cfg.Label
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.JetElementInstructionImpl
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionImpl
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.Instruction
import org.jetbrains.jet.utils.emptyOrSingletonList

public abstract class AbstractJumpInstruction(
        element: JetElement,
        public val targetLabel: Label,
        lexicalScope: LexicalScope
) : JetElementInstructionImpl(element, lexicalScope), JumpInstruction {
    public var resolvedTarget: Instruction? = null
        set(value: Instruction?) {
            $resolvedTarget = outgoingEdgeTo(value)
        }

    protected abstract fun createCopy(newLabel: Label, lexicalScope: LexicalScope): AbstractJumpInstruction

    public fun copy(newLabel: Label): Instruction {
        return updateCopyInfo(createCopy(newLabel, lexicalScope))
    }

    override fun createCopy(): InstructionImpl {
        return createCopy(targetLabel, lexicalScope)
    }

    override val nextInstructions: Collection<Instruction>
        get() = emptyOrSingletonList(resolvedTarget)
}