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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions.special

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitorWithResult

public class CompilationErrorInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val message: String
) : InstructionWithNext(element, lexicalScope) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCompilationErrorInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitCompilationErrorInstruction(this)
    }

    override fun createCopy() = CompilationErrorInstruction(element, lexicalScope, message)

    override fun toString() = "error(${render(element)}, $message)"
}