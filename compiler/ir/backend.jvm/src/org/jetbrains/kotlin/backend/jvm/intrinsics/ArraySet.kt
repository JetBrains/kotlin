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
import org.jetbrains.kotlin.backend.jvm.ir.getArrayElementType
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Type

object ArraySet : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val receiver = expression.dispatchReceiver!!.accept(codegen, data).materialized
        val elementType = AsmUtil.correctElementType(receiver.type)
        val elementIrType = receiver.irType.getArrayElementType(codegen.context.irBuiltIns)
        expression.getValueArgument(0)!!.accept(codegen, data).coerce(Type.INT_TYPE, codegen.context.irBuiltIns.intType).materialize()
        expression.getValueArgument(1)!!.accept(codegen, data).coerce(elementType, elementIrType).materialize()
        codegen.mv.astore(elementType)
        return codegen.immaterialUnitValue
    }
}
