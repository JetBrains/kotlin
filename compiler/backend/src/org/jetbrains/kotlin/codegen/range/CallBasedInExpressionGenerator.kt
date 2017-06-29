/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class CallBasedInExpressionGenerator(
        val codegen: ExpressionCodegen,
        operatorReference: KtSimpleNameExpression
) : InExpressionGenerator {
    private val resolvedCall = operatorReference.getResolvedCallWithAssert(codegen.bindingContext)
    private val isInverted =  operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN

    override fun generate(argument: StackValue): BranchedValue =
            gen(argument).let { if (isInverted) Invert(it) else it }

    private fun gen(argument: StackValue): BranchedValue =
            object : BranchedValue(argument, null, argument.type, Opcodes.IFEQ) {
                override fun putSelector(type: Type, v: InstructionAdapter) {
                    invokeFunction(v)
                }

                override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                    invokeFunction(v)
                    v.visitJumpInsn(if (jumpIfFalse) Opcodes.IFEQ else Opcodes.IFNE, jumpLabel)
                }

                private fun invokeFunction(v: InstructionAdapter) {
                    val result = codegen.invokeFunction(resolvedCall.call, resolvedCall, none())
                    result.put(result.type, v)
                }
            }
}