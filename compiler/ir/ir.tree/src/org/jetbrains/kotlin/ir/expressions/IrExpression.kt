/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions

import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer

abstract class IrExpression : IrElementBase(), IrStatement, IrVarargElement, IrAttributeContainer {
    @Suppress("LeakingThis")
    override var attributeOwnerId: IrAttributeContainer = this

    abstract var type: IrType

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrExpression =
        accept(transformer, data) as IrExpression
}
