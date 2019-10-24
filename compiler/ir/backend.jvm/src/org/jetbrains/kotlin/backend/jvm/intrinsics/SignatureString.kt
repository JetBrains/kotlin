/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.AsmTypes

/**
 * Computes the JVM signature of a given IrFunction. The function is passed as an IrFunctionReference
 * to the single argument of the intrinsic.
 */
object SignatureString : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val function = (expression.getValueArgument(0) as IrFunctionReference).symbol.owner

        // TODO do not use descriptors
        val declaration = codegen.context.referenceFunction(
            DescriptorUtils.unwrapFakeOverride(function.descriptor).original
        ).owner

        val method = codegen.context.methodSignatureMapper.mapAsmMethod(declaration)
        val descriptor = method.name + method.descriptor

        return object : PromisedValue(codegen, AsmTypes.JAVA_STRING_TYPE, codegen.context.irBuiltIns.stringType) {
            override fun materialize() {
                codegen.mv.aconst(descriptor)
            }
        }
    }
}
