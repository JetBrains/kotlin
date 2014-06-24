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
import com.google.common.collect.Maps
import com.google.common.collect.Lists
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.JetElementInstructionImpl
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.Instruction
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionImpl

public class NondeterministicJumpInstruction(
        element: JetElement,
        targetLabels: List<Label>,
        lexicalScope: LexicalScope,
        public val inputValue: PseudoValue?
) : JetElementInstructionImpl(element, lexicalScope), JumpInstruction {
    private var _next: Instruction? = null
    private val _resolvedTargets: MutableMap<Label, Instruction> = Maps.newLinkedHashMap()

    public val targetLabels: List<Label> = Lists.newArrayList(targetLabels)
    public val resolvedTargets: Map<Label, Instruction>
            get() = _resolvedTargets

    public fun setResolvedTarget(label: Label, resolvedTarget: Instruction) {
        _resolvedTargets[label] = outgoingEdgeTo(resolvedTarget)!!
    }

    public var next: Instruction
        get() = _next!!
        set(value: Instruction) {
            _next = outgoingEdgeTo(value)
        }

    override val nextInstructions: Collection<Instruction>
        get() {
            val targetInstructions = Lists.newArrayList(resolvedTargets.values())
            targetInstructions.add(next)
            return targetInstructions
        }

    override val inputValues: List<PseudoValue>
        get() = ContainerUtil.createMaybeSingletonList(inputValue)

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitNondeterministicJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitNondeterministicJump(this)
    }

    override fun toString(): String {
        val inVal = if (inputValue != null) "|$inputValue" else ""
        val labels = targetLabels.map { it.getName() }.joinToString(", ")
        return "jmp?($labels$inVal)"
    }

    override fun createCopy(): InstructionImpl {
        return createCopy(targetLabels)
    }

    public fun copy(newTargetLabels: MutableList<Label>): Instruction {
        return updateCopyInfo(createCopy(newTargetLabels))
    }

    private fun createCopy(newTargetLabels: List<Label>): InstructionImpl {
        return NondeterministicJumpInstruction(element, newTargetLabels, lexicalScope, inputValue)
    }
}