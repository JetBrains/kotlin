/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.unboxInlineClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

/**
 * Handles `ReflectiveAccessLowering`-originated reflective access.
 *
 * Those need special handling.
 * Without it, we would try to unbox the result of the reflective access as it has `java.lang.Object` type.
 * In fact, the result is either an unboxed value or wrapped one (in case of primitive).
 * Thus, we simply cast it to the target type and also unwrap it in the latter case.
 */

object HandleResultOfReflectiveAccess : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue {
        val typeMapper = codegen.typeMapper
        val mv = codegen.mv
        val type = expression.getTypeArgument(0)!!
        expression.getValueArgument(0)!!.accept(codegen, data).materialize()
        val asmResultType = typeMapper.mapType(type.unboxInlineClass())
        val castToType = AsmUtil.boxType(asmResultType)
        mv.checkcast(castToType)
        if (AsmUtil.isPrimitive(asmResultType)) StackValue.coerce(castToType, asmResultType, mv)
        return MaterialValue(codegen, asmResultType, type)
    }
}
