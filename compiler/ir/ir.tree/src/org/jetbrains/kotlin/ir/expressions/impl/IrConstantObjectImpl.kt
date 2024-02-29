/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.expressions.impl

import org.jetbrains.kotlin.ir.declarations.IrAttributeContainer
import org.jetbrains.kotlin.ir.expressions.IrConstantObject
import org.jetbrains.kotlin.ir.expressions.IrConstantValue
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.constructedClassType
import org.jetbrains.kotlin.utils.SmartList

class IrConstantObjectImpl(
    override val startOffset: Int,
    override val endOffset: Int,
    override var constructor: IrConstructorSymbol,
    initValueArguments: List<IrConstantValue>,
    initTypeArguments: List<IrType>,
    override var type: IrType = constructor.owner.constructedClassType,
) : IrConstantObject() {
    override val valueArguments = SmartList(initValueArguments)
    override val typeArguments = SmartList(initTypeArguments)

    override var attributeOwnerId: IrAttributeContainer = this
    override var originalBeforeInline: IrAttributeContainer? = null
}
