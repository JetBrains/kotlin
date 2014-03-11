/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode

import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall

class CallInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val resolvedCall: ResolvedCall<*>
) : InstructionWithNext(element, lexicalScope) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitCallInstruction(this)
    }

    override fun createCopy() = CallInstruction(element, lexicalScope, resolvedCall)

    override fun toString() = "call(${render(element)}, ${resolvedCall.getResultingDescriptor()!!.getName()})"
}

class CompilationErrorInstruction(
        element: JetElement,
        lexicalScope: LexicalScope,
        val message: String
) : InstructionWithNext(element, lexicalScope) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCompilationErrorInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitCompilationErrorInstruction(this)
    }

    override fun createCopy() = CompilationErrorInstruction(element, lexicalScope, message)

    override fun toString() = "error(${render(element)}, $message)"
}

// This instruciton is used to let the dead code detector know the syntactic structure of unreachable code
// otherwise only individual parts of expression would be reported as unreachable
// e.g. for (i in foo) {} -- only i and foo would be marked unreachable
class MarkInstruction(
        element: JetElement,
        lexicalScope: LexicalScope
) : InstructionWithNext(element, lexicalScope) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitMarkInstruction(this)
    }

    override fun <R> accept(visitor: InstructionVisitorWithResult<R>): R? {
        return visitor.visitMarkInstruction(this)
    }

    override fun createCopy() = MarkInstruction(element, lexicalScope)

    override fun toString() = "mark(${render(element)})"
}