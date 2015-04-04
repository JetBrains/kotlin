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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ExtendedCallable
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.context.CodegenContext
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE

public class MonitorInstruction private(private val opcode: Int) : IntrinsicMethod() {
    companion object {
        public val MONITOR_ENTER: MonitorInstruction = MonitorInstruction(Opcodes.MONITORENTER)
        public val MONITOR_EXIT: MonitorInstruction = MonitorInstruction(Opcodes.MONITOREXIT)
    }

    override fun generateImpl(codegen: ExpressionCodegen, v: InstructionAdapter, returnType: Type, element: PsiElement?, arguments: List<JetExpression>, receiver: StackValue): Type {
        assert(element != null, "Element should not be null")

        val resolvedCall = (element as JetElement).getResolvedCallWithAssert(codegen.getBindingContext())

        val resolvedArguments = resolvedCall.getValueArgumentsByIndex()
        assert(resolvedArguments != null && resolvedArguments.size() == 1) { "Monitor instruction (" + opcode + ") should have exactly 1 argument: " + resolvedArguments }

        val argument = resolvedArguments!!.get(0)
        assert(argument is ExpressionValueArgument) { "Monitor instruction (" + opcode + ") should have expression value argument: " + argument }

        val valueArgument = (argument as ExpressionValueArgument).getValueArgument()
        assert(valueArgument != null) { "Unresolved value argument: " + argument }
        codegen.gen(valueArgument!!.getArgumentExpression(), OBJECT_TYPE)

        v.visitInsn(opcode)
        return Type.VOID_TYPE
    }

    override fun supportCallable(): Boolean {
        return true
    }

    override fun toCallable(state: GenerationState, fd: FunctionDescriptor, context: CodegenContext<*>, isSuper: Boolean, resolvedCall: ResolvedCall<*>): ExtendedCallable {
        return object : IntrinsicCallable(Type.VOID_TYPE, listOf(OBJECT_TYPE), null, null) {
            override fun invokeIntrinsic(v: InstructionAdapter) {
                v.visitInsn(opcode)
            }
        }
    }
}
