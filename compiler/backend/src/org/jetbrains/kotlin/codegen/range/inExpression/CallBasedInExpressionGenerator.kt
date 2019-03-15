/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range.inExpression

import org.jetbrains.kotlin.codegen.BranchedValue
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.Invert
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class CallBasedInExpressionGenerator(
    val codegen: ExpressionCodegen,
    operatorReference: KtSimpleNameExpression
) : InExpressionGenerator {
    private val resolvedCall = operatorReference.getResolvedCallWithAssert(codegen.bindingContext)
    private val isInverted = operatorReference.getReferencedNameElementType() == KtTokens.NOT_IN

    override fun generate(argument: StackValue): BranchedValue =
        gen(argument).let { if (isInverted) Invert(it) else it }

    private fun gen(argument: StackValue): BranchedValue =
        object : BranchedValue(argument, null, argument.type, Opcodes.IFEQ) {
            override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                invokeFunction(v)
                coerceTo(type, kotlinType, v)
            }

            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                invokeFunction(v)
                v.visitJumpInsn(if (jumpIfFalse) Opcodes.IFEQ else Opcodes.IFNE, jumpLabel)
            }

            private fun invokeFunction(v: InstructionAdapter) {
                val result = codegen.invokeFunction(resolvedCall.call, resolvedCall, none())
                result.put(result.type, result.kotlinType, v)
            }
        }
}