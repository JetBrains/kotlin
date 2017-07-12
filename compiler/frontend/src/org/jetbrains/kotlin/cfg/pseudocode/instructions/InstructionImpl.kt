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

package org.jetbrains.kotlin.cfg.pseudocode.instructions

import java.util.Collections
import java.util.LinkedHashSet
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue

abstract class InstructionImpl(override val blockScope: BlockScope): Instruction {
    private var _owner: Pseudocode? = null

    override var owner: Pseudocode
        get() = _owner!!
        set(value) {
            assert(_owner == null || _owner == value)
            _owner = value
        }

    private var allCopies: MutableSet<InstructionImpl>? = null

    override val copies: Collection<Instruction>
        get() = allCopies?.filter { it != this } ?: Collections.emptyList()

    fun copy(): Instruction = updateCopyInfo(createCopy())

    protected abstract fun createCopy(): InstructionImpl

    protected fun updateCopyInfo(instruction: InstructionImpl): Instruction {
        if (allCopies == null) {
            allCopies = hashSetOf(this)
        }
        instruction.allCopies = allCopies
        allCopies!!.add(instruction)
        return instruction
    }

    var markedAsDead: Boolean = false

    override val dead: Boolean get() = allCopies?.all { it.markedAsDead } ?: markedAsDead

    override val previousInstructions: MutableCollection<Instruction> = LinkedHashSet()

    protected fun outgoingEdgeTo(target: Instruction?): Instruction? {
        (target as InstructionImpl?)?.previousInstructions?.add(this)
        return target
    }

    override val inputValues: List<PseudoValue> = Collections.emptyList()
}
