/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionBase
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartList

interface IrBuiltinWithMangle : IrDeclaration, IrSymbolOwner {
    val mangle: String
}

class IrBuiltInOperator(
    override val symbol: IrSimpleFunctionSymbol,
    name: Name,
    returnType: IrType,
    val suffix: String
) :
    IrSimpleFunction,
    IrBuiltinWithMangle,
    IrFunctionBase(
        UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrBuiltIns.BUILTIN_OPERATOR,
        name, Visibilities.PUBLIC, false, false, false,
        returnType
    ) {

    override val modality get() = Modality.FINAL
    override val isTailrec get() = false
    override val isSuspend get() = false
    override val isFakeOverride get() = false
    override val isOperator: Boolean get() = false
    override var correspondingPropertySymbol: IrPropertySymbol?
        get() = null
        set(_) {}
    override val descriptor: FunctionDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSimpleFunction(this, data)
    }

    override val overriddenSymbols: MutableList<IrSimpleFunctionSymbol> = SmartList()
    override val mangle: String get() = "operator#$name@$suffix"

    init {
        symbol.bind(this)
    }
}

class IrBuiltInOperatorValueParameter(override val symbol: IrValueParameterSymbol, override val index: Int, override val type: IrType) :
    IrValueParameter, IrDeclarationBase(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrBuiltIns.BUILTIN_OPERATOR) {

    override val descriptor: ParameterDescriptor get() = symbol.descriptor
    override val varargElementType: IrType? get() = null
    override val isCrossinline: Boolean get() = false
    override val isNoinline: Boolean get() = false
    override var defaultValue: IrExpressionBody?
        get() = null
        set(_) {}
    override val name: Name = Name.identifier("arg$index")

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D) =
        transformer.visitValueParameter(this, data) as IrValueParameter

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = visitor.visitValueParameter(this, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {}
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {}

    init {
        symbol.bind(this)
    }
}

class IrBuiltInOperatorTypeParameter(
    override val symbol: IrTypeParameterSymbol,
    override val variance: Variance,
    override val index: Int,
    override val isReified: Boolean
) : IrTypeParameter, IrDeclarationBase(UNDEFINED_OFFSET, UNDEFINED_OFFSET, IrBuiltIns.BUILTIN_OPERATOR) {

    override val descriptor: TypeParameterDescriptor get() = symbol.descriptor
    override val superTypes: MutableList<IrType> = SmartList()
    override val name: Name = Name.identifier("T$index")

    override fun <D> transform(transformer: IrElementTransformer<D>, data: D): IrTypeParameter =
        transformer.visitTypeParameter(this, data) as IrTypeParameter

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R = visitor.visitTypeParameter(this, data)
    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {}
    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {}

    init {
        symbol.bind(this)
    }
}