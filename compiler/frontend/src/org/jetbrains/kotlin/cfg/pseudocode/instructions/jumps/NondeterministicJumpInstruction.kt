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

import org.jetbrains.kotlin.cfg.pseudocode.PseudoValue
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.cfg.Label
import com.google.common.collect.Maps
import com.google.common.collect.Lists
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.KtElementInstructionImpl
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl
import org.jetbrains.kotlin.utils.emptyOrSingletonList

class NondeterministicJumpInstruction(
        element: KtElement,
        targetLabels: List<Label>,
        blockScope: BlockScope,
        val inputValue: PseudoValue?
) : KtElementInstructionImpl(element, blockScope), JumpInstruction {
    private var _next: Instruction? = null
    private val _resolvedTargets: MutableMap<Label, Instruction> = Maps.newLinkedHashMap()

    val targetLabels: List<Label> = Lists.newArrayList(targetLabels)
    val resolvedTargets: Map<Label, Instruction>
            get() = _resolvedTargets

    fun setResolvedTarget(label: Label, resolvedTarget: Instruction) {
        _resolvedTargets[label] = outgoingEdgeTo(resolvedTarget)!!
    }

    var next: Instruction
        get() = _next!!
        set(value: Instruction) {
            _next = outgoingEdgeTo(value)
        }

    override val nextInstructions: Collection<Instruction>
        get() {
            val targetInstructions = Lists.newArrayList(resolvedTargets.values)
            targetInstructions.add(next)
            return targetInstructions
        }

    override val inputValues: List<PseudoValue>
        get() = emptyOrSingletonList(inputValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitNondeterministicJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitNondeterministicJump(this)
    }

    override fun toString(): String {
        val inVal = if (inputValue != null) "|$inputValue" else ""
        val labels = targetLabels.map { it.name }.joinToString(", ")
        return "jmp?($labels$inVal)"
    }

    override fun createCopy(): InstructionImpl {
        return createCopy(targetLabels)
    }

    fun copy(newTargetLabels: MutableList<Label>): Instruction {
        return updateCopyInfo(createCopy(newTargetLabels))
    }

    private fun createCopy(newTargetLabels: List<Label>): InstructionImpl {
        return NondeterministicJumpInstruction(element, newTargetLabels, blockScope, inputValue)
    }
}
