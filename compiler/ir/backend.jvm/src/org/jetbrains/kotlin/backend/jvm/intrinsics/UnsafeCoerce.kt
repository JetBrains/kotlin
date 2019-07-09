/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression

/**
 * Implicit coercion between IrTypes with the same underlying representation.
 *
 * A call of the form `coerce<A,B>(x)` allows us to coerce the value of `x` to type `A` but treat the result as if
 * it had IrType `B`. This is useful for inline classes, whose coercion behavior depends on the IrType in
 * addition to the underlying asmType.
 */
object UnsafeCoerce : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val from = expression.getTypeArgument(0)!!
        val to = expression.getTypeArgument(1)!!
        require(with(codegen) { typeMapper.mapType(from) == typeMapper.mapType(to) })
        val arg = expression.getValueArgument(0)!!
        val result = arg.accept(codegen, data)
        return object : PromisedValue(codegen, codegen.typeMapper.mapType(to), to) {
            override fun materialize() = result.coerce(from).materialize()
        }
    }
}
