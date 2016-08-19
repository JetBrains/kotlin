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

import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.BlockScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl
import org.jetbrains.kotlin.psi.*

class VariableDeclarationInstruction(
        element: KtDeclaration,
        blockScope: BlockScope
) : InstructionWithNext(element, blockScope) {
    init {
        assert(element is KtVariableDeclaration || element is KtParameter || element is KtEnumEntry || element is KtObjectDeclaration) {
            "Invalid element: ${render(element)}}"
        }
    }

    val variableDeclarationElement: KtDeclaration
        get() = element as KtDeclaration

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitVariableDeclarationInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitVariableDeclarationInstruction(this)
    }

    override fun toString(): String = "v(${render(element)})"

    override fun createCopy(): InstructionImpl =
            VariableDeclarationInstruction(variableDeclarationElement, blockScope)
}
