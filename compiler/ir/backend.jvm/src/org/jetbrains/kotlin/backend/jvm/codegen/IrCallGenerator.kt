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

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.ValueKind
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Type

interface IrCallGenerator {
    fun genCall(callableMethod: IrCallableMethod, codegen: ExpressionCodegen, expression: IrFunctionAccessExpression) {
        with(callableMethod) {
            codegen.mv.visitMethodInsn(invokeOpcode, owner.internalName, asmMethod.name, asmMethod.descriptor, isInterfaceMethod)
        }
    }

    fun beforeValueParametersStart() {

    }

    fun genValueAndPut(
        irValueParameter: IrValueParameter,
        argumentExpression: IrExpression,
        parameterType: Type,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo
    ) {
        codegen.gen(argumentExpression, parameterType, irValueParameter.type, blockInfo)
    }

    fun putValueIfNeeded(parameterType: Type, value: StackValue, kind: ValueKind, parameterIndex: Int, codegen: ExpressionCodegen) {
        value.put(parameterType, codegen.visitor)
    }

    object DefaultCallGenerator : IrCallGenerator
}
