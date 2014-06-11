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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions

import java.util.Collections
import java.util.LinkedHashSet
import java.util.HashSet
import com.google.common.collect.Sets
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue

public abstract class InstructionImpl(public override val lexicalScope: LexicalScope): Instruction {
    private var _owner: Pseudocode? = null
    private val _copies = HashSet<Instruction>()
    private var original: Instruction? = null
    protected var _dead: Boolean = false

    private fun setOriginalInstruction(value: Instruction?) {
        assert(original == null) { "Instruction can't have two originals: this.original = ${original}; new original = $this" }
        original = value
    }

    protected fun outgoingEdgeTo(target: Instruction?): Instruction? {
        if (target != null) {
            target.previousInstructions.add(this)
        }
        return target
    }

    protected fun updateCopyInfo(instruction: InstructionImpl): Instruction {
        _copies.add(instruction)
        instruction.setOriginalInstruction(this)
        return instruction
    }

    protected abstract fun createCopy(): InstructionImpl

    public fun die() {
        _dead = true
    }

    override val dead: Boolean get() = _dead

    public fun copy(): Instruction {
        return updateCopyInfo(createCopy())
    }

    override var owner: Pseudocode
        get() = _owner!!
        set(value: Pseudocode) {
            assert(_owner == null || _owner == value)
            _owner = value
        }

    override val previousInstructions: MutableCollection<Instruction> = LinkedHashSet()

    override val inputValues: List<PseudoValue> = Collections.emptyList()

    override fun getCopies(): Collection<Instruction> {
        return original?.let { original ->
            val originalCopies = Sets.newHashSet(original.getCopies())
            originalCopies.remove(this)
            originalCopies.add(original)
            originalCopies
        } ?: _copies
    }
}