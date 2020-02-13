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
import org.jetbrains.kotlin.codegen.AsmUtil.boxType
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.Type

object JavaClassProperty : IntrinsicMethod() {
    private fun invokeGetClass(value: PromisedValue) {
        value.mv.invokevirtual("java/lang/Object", "getClass", "()Ljava/lang/Class;", false)
    }

    fun invokeWith(value: PromisedValue) =
        when {
            value.type == Type.VOID_TYPE ->
                invokeGetClass(value.materializedAt(AsmTypes.UNIT_TYPE, value.codegen.context.irBuiltIns.unitType))
            value.irType.classOrNull?.owner?.isInline == true ->
                invokeGetClass(value.materializedAtBoxed(value.irType))
            isPrimitive(value.type) -> {
                value.discard()
                value.mv.getstatic(boxType(value.type).internalName, "TYPE", "Ljava/lang/Class;")
            }
            else ->
                invokeGetClass(value.materialized())
        }

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        invokeWith(expression.extensionReceiver!!.accept(codegen, data))
        return with(codegen) { expression.onStack }
    }
}
