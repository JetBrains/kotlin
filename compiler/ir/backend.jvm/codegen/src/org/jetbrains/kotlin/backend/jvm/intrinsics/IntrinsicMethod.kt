/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.IntrinsicMarker
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

abstract class IntrinsicMethod : IntrinsicMarker {
    open fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IntrinsicFunction = TODO("implement toCallable() or invoke() of $this")

    open fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? =
        with(codegen) {
            val descriptor = methodSignatureMapper.mapSignatureSkipGeneric(expression.symbol.owner)
            val stackValue = toCallable(expression, descriptor, codegen.classCodegen).invoke(mv, codegen, data, expression)
            stackValue.put(mv)
            return MaterialValue(this, stackValue.type, expression.type)
        }


    companion object {
        fun JvmMethodSignature.newReturnType(type: Type): JvmMethodSignature {
            val newMethod = with(asmMethod) {
                Method(name, type, argumentTypes)
            }
            return JvmMethodSignature(newMethod, valueParameters)
        }
    }
}
