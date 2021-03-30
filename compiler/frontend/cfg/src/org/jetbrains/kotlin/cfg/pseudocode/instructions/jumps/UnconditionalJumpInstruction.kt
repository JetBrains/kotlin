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

import org.jetbrains.kotlin.cfg.pseudocode.instructions.*
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.cfg.Label

class UnconditionalJumpInstruction(
    element: KtElement,
    targetLabel: Label,
    blockScope: BlockScope
) : AbstractJumpInstruction(element, targetLabel, blockScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitUnconditionalJump(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R = visitor.visitUnconditionalJump(this)

    override fun toString(): String = "jmp(${targetLabel.name})"

    override fun createCopy(newLabel: Label, blockScope: BlockScope): AbstractJumpInstruction =
        UnconditionalJumpInstruction(element, newLabel, blockScope)
}
