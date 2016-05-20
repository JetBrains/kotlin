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

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode
import com.google.common.collect.Lists
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl

class LocalFunctionDeclarationInstruction(
        element: KtElement,
        val body: Pseudocode,
        blockScope: BlockScope
) : InstructionWithNext(element, blockScope) {
    var sink: SubroutineSinkInstruction? = null
        set(value: SubroutineSinkInstruction?) {
            field = outgoingEdgeTo(value) as SubroutineSinkInstruction?
        }

    override val nextInstructions: Collection<Instruction>
        get() {
            if (sink != null) {
                val instructions = Lists.newArrayList<Instruction>(sink)
                instructions.addAll(super.nextInstructions)
                return instructions
            }
            return super.nextInstructions
        }

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitLocalFunctionDeclarationInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitLocalFunctionDeclarationInstruction(this)
    }

    override fun toString(): String = "d(${render(element)})"

    override fun createCopy(): InstructionImpl =
            LocalFunctionDeclarationInstruction(element, body.copy(), blockScope)
}
