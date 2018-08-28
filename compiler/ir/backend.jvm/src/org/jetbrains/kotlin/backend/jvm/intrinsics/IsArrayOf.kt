/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.Variance

class IsArrayOf : IntrinsicMethod() {
    override fun toCallable(expression: IrMemberAccessExpression, signature: JvmMethodSignature, context: JvmBackendContext): IrIntrinsicFunction {
        val typeMapper = context.state.typeMapper

        val descriptor = expression.descriptor
        val builtIns = descriptor.module.builtIns
        assert(descriptor.typeParameters.size == 1) {
            "Expected only one type parameter for Any?.isArrayOf(), got: ${descriptor.typeParameters}"
        }
        /*TODO original?*/
        val elementType = expression.getTypeArgument(descriptor.original.typeParameters.first().index)!!
        val arrayKtType = builtIns.getArrayType(Variance.INVARIANT, elementType.toKotlinType())
        val arrayType = typeMapper.mapType(arrayKtType)

        return IrIntrinsicFunction.create(expression, signature, context) {
            it.instanceOf(arrayType)
        }
    }
}