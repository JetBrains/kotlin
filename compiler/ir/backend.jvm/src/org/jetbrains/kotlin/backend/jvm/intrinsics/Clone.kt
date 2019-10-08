/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Opcodes

object Clone : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo) = with(codegen) {
        val result = expression.dispatchReceiver!!.accept(this, data).materialized
        assert(!AsmUtil.isPrimitive(result.type)) { "clone() of primitive type" }
        val opcode = if (expression is IrCall && expression.superQualifierSymbol != null) Opcodes.INVOKESPECIAL else Opcodes.INVOKEVIRTUAL
        mv.visitMethodInsn(opcode, "java/lang/Object", "clone", "()Ljava/lang/Object;", false)
        MaterialValue(codegen, AsmTypes.OBJECT_TYPE, context.irBuiltIns.anyNType)
    }
}
