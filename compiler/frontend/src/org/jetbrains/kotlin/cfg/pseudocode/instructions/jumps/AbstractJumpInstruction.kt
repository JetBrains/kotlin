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

package org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.cfg.Label
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstructionImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.utils.emptyOrSingletonList

abstract class AbstractJumpInstruction(
        element: KtElement,
        val targetLabel: Label,
        blockScope: BlockScope
) : KtElementInstructionImpl(element, blockScope), JumpInstruction {
    var resolvedTarget: Instruction? = null
        set(value: Instruction?) {
            field = outgoingEdgeTo(value)
        }

    protected abstract fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction

    fun copy(newLabel: Label): Instruction {
        return updateCopyInfo(createCopy(newLabel, blockScope))
    }

    override fun createCopy(): InstructionImpl {
        return createCopy(targetLabel, blockScope)
    }

    override val nextInstructions: Collection<Instruction>
        get() = emptyOrSingletonList(resolvedTarget)
}
