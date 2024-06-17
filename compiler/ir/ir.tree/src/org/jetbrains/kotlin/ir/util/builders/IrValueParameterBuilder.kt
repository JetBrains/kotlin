/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util.builders

import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.name.Name

class IrValueParameterBuilder : IrDeclarationBuilder<IrValueParameter> {
    var type: IrType
    var varargElementType: IrType?
    var isCrossinline: Boolean
    var isNoinline: Boolean
    var isHidden: Boolean
    var isAssignable: Boolean
    var defaultValue: IrExpressionBody? = null
    val symbol: IrValueParameterSymbol = IrValueParameterSymbolImpl()

    @PublishedApi
    internal constructor(name: Name, type: IrType) : super(name) {
        this.type = type
        varargElementType = null
        isCrossinline = false
        isNoinline = false
        isHidden = false
        isAssignable = false
    }

    @PublishedApi
    internal constructor(type: IrType, from: IrValueParameter) : super(from.name, from) {
        this.type = type
        varargElementType = from.varargElementType
        isCrossinline = from.isCrossinline
        isNoinline = from.isNoinline
        isHidden = from.isHidden
        isAssignable = from.isAssignable
    }

    @PublishedApi
    internal fun build(factory: IrFactory, index: Int, irFunction: IrFunction): IrValueParameter =
        factory.createValueParameter(
            startOffset = if (startOffset == UNDEFINED_OFFSET) irFunction.startOffset else startOffset,
            endOffset = if (endOffset == UNDEFINED_OFFSET) irFunction.endOffset else endOffset,
            origin = origin,
            name = name,
            type = type,
            isAssignable = isAssignable,
            symbol = symbol,
            index = index,
            varargElementType = varargElementType,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline,
            isHidden = isHidden,
        ).also {
            it.parent = irFunction
            it.defaultValue = defaultValue?.patchDeclarationParents(irFunction)
        }
}