/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg.pseudocode

import org.jetbrains.kotlin.cfg.Label
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.psi.KtElement

class PseudocodeLabel internal constructor(
        override val pseudocode: PseudocodeImpl, override val name: String, private val comment: String?
) : Label {

    private val instructionList: List<Instruction> get() = pseudocode.mutableInstructionList

    private val correspondingElement: KtElement get() = pseudocode.correspondingElement

    override var targetInstructionIndex = -1

    override fun toString(): String {
        return if (comment == null) name else "$name [$comment]"
    }

    override fun resolveToInstruction(): Instruction {
        val index = targetInstructionIndex
        if (index < 0 || index >= instructionList.size) {
            error("resolveToInstruction: incorrect index $index for label $name " +
                  "in subroutine ${correspondingElement.text} with instructions $instructionList")
        }
        return instructionList[index]
    }

    fun copy(newPseudocode: PseudocodeImpl, newLabelIndex: Int): PseudocodeLabel {
        return PseudocodeLabel(newPseudocode, "L" + newLabelIndex, "copy of $name, $comment")
    }
}
