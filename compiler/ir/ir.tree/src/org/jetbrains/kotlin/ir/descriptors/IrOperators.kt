/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.descriptors

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
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
import kotlin.reflect.KProperty

abstract class IrBuiltInOperatorBase : IrDeclaration {
    override val startOffset: Int get() = UNDEFINED_OFFSET
    override val endOffset: Int get() = UNDEFINED_OFFSET

    override var origin: IrDeclarationOrigin
        get() = IrBuiltIns.BUILTIN_OPERATOR
        set(_) {}

    private var _parent: IrDeclarationParent? = null
    override var parent: IrDeclarationParent
        get() = _parent
            ?: throw UninitializedPropertyAccessException("Parent not initialized: $this")
        set(v) {
            _parent = v
        }

    override var annotations: List<IrConstructorCall> = emptyList()
    override val metadata: MetadataSource? get() = null
}

private class NullValueDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = null
    operator fun setValue(thisRef: Any?, property: KProperty<*>, v: T?) {
        error("IrBuiltInOperators are not supposed to be mutated")
    }
}

class IrBuiltInOperator(
    override val symbol: IrSimpleFunctionSymbol,
    override val name: Name,
    override var returnType: IrType
) :
    IrSimpleFunction,
    IrFunction,
    IrBuiltInOperatorBase() {

    override val visibility: Visibility get() = Visibilities.PUBLIC

    override val isInline: Boolean get() = false
    override val isExternal: Boolean get() = false
    override val isExpect: Boolean get() = false

    override var dispatchReceiverParameter: IrValueParameter? by NullValueDelegate()

    override var extensionReceiverParameter: IrValueParameter? by NullValueDelegate()

    override var body: IrBody? by NullValueDelegate()

    override var valueParameters: List<IrValueParameter> = emptyList()
    override var typeParameters: List<IrTypeParameter> = emptyList()

    override val modality get() = Modality.FINAL
    override val isTailrec get() = false
    override val isSuspend get() = false
    override val isFakeOverride get() = false
    override val isOperator: Boolean get() = false
    override var correspondingPropertySymbol: IrPropertySymbol? by NullValueDelegate()

    override val descriptor: FunctionDescriptor get() = symbol.descriptor

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSimpleFunction(this, data)
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> = emptyList()
    override var attributeOwnerId: IrAttributeContainer = this

    init {
        symbol.bind(this)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        // Do nothing
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        // Do nothing
    }
}

class IrBuiltInOperatorValueParameter(override val symbol: IrValueParameterSymbol, override val index: Int, override val type: IrType) :
    IrValueParameter, IrBuiltInOperatorBase() {

    override val descriptor: ParameterDescriptor get() = symbol.descriptor
    override val varargElementType: IrType? get() = null
    override val isCrossinline: Boolean get() = false
    override val isNoinline: Boolean get() = false
    override var defaultValue: IrExpressionBody? by NullValueDelegate()

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
) : IrTypeParameter, IrBuiltInOperatorBase() {

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