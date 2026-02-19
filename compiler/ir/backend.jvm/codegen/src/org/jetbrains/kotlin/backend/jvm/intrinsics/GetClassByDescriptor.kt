/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.MaterialValue
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.org.objectweb.asm.Type

object GetClassByDescriptor : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val type = Type.getType(expression.getStringConstArgument(0))
        require(type.sort != Type.VOID) { "Unexpected VOID type descriptor" }
        if (AsmUtil.isPrimitive(type)) {
            codegen.mv.getstatic(AsmUtil.boxType(type).internalName, "TYPE", "Ljava/lang/Class;")
        } else {
            codegen.mv.aconst(type)
        }
        return MaterialValue(codegen, codegen.typeMapper.mapType(expression.type), expression.type)
    }
}