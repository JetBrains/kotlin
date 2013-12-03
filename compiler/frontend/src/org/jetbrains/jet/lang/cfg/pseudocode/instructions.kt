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
        val resolvedCall: ResolvedCall<*>
) : InstructionWithNext(element) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCallInstruction(this)
    }

    override fun createCopy() = CallInstruction(element, resolvedCall)

    override fun toString() = "call(${render(element)}, ${resolvedCall.getResultingDescriptor()!!.getName()})"
}

class CompilationErrorInstruction(
        element: JetElement,
        val message: String
) : InstructionWithNext(element) {

    override fun accept(visitor: InstructionVisitor) {
        visitor.visitCompilationErrorInstruction(this)
    }

    override fun createCopy() = CompilationErrorInstruction(element, message)

    override fun toString() = "error(${render(element)}, $message)"
}