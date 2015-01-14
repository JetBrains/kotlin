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

package org.jetbrains.kotlin.cfg.pseudocode.instructions.special

import org.jetbrains.kotlin.psi.JetDeclaration
import org.jetbrains.kotlin.psi.JetVariableDeclaration
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.kotlin.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionVisitorWithResult
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl

public class VariableDeclarationInstruction(
        element: JetDeclaration,
        lexicalScope: LexicalScope
) : InstructionWithNext(element, lexicalScope) {
    {
        assert(element is JetVariableDeclaration || element is JetParameter) { "Invalid element: ${render(element)}}" }
    }

    public val variableDeclarationElement: JetDeclaration
        get() = element as JetDeclaration

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitVariableDeclarationInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitVariableDeclarationInstruction(this)
    }

    override fun toString(): String = "v(${render(element)})"

    override fun createCopy(): InstructionImpl =
            VariableDeclarationInstruction(variableDeclarationElement, lexicalScope)
}
