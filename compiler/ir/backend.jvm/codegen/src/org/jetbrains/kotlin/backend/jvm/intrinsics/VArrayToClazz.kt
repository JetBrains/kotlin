/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.ClassCodegen
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.isVArray
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type


object VArrayToClazz : IntrinsicMethod() {
    override fun toCallable(
        expression: IrFunctionAccessExpression,
        signature: JvmMethodSignature,
        classCodegen: ClassCodegen
    ): IrIntrinsicFunction {
        var type = expression.getTypeArgument(0) as IrSimpleType
        var vArrayDimensionsCount = 0
        while (type.isVArray) {
            vArrayDimensionsCount++
            type = type.arguments[0] as IrSimpleType
        }
        var asmType = classCodegen.typeMapper.mapType(type, TypeMappingMode.CLASS_DECLARATION)
        while (vArrayDimensionsCount-- > 0) {
            asmType = AsmUtil.getArrayType(asmType)
        }
        return IrIntrinsicFunction.create(expression, signature, classCodegen) {
            it.aconst(asmType)
        }
    }
}