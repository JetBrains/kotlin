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
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionImpl

public class UnsupportedElementInstruction(
        element: JetElement,
        lexicalScope: LexicalScope
) : InstructionWithNext(element, lexicalScope) {
    override fun accept(visitor: InstructionVisitor) {
        visitor.visitUnsupportedElementInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R {
        return visitor.visitUnsupportedElementInstruction(this)
    }

    override fun toString(): String =
            "unsupported(" + element + " : " + render(element) + ")"

    override fun createCopy(): InstructionImpl =
            UnsupportedElementInstruction(element, lexicalScope)
}