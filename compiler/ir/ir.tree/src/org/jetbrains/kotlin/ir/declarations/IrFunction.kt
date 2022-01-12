/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor

abstract class IrFunction :
    IrDeclarationBase(),
    IrPossiblyExternalDeclaration, IrDeclarationWithVisibility, IrTypeParametersContainer, IrSymbolOwner, IrDeclarationParent, IrReturnTarget,
    IrMemberWithContainerSource,
    IrMetadataSourceOwner {

    @ObsoleteDescriptorBasedAPI
    abstract override val descriptor: FunctionDescriptor
    abstract override val symbol: IrFunctionSymbol

    abstract val isInline: Boolean // NB: there's an inline constructor for Array and each primitive array class
    abstract val isExpect: Boolean

    abstract var returnType: IrType

    abstract var dispatchReceiverParameter: IrValueParameter?
    abstract var extensionReceiverParameter: IrValueParameter?
    abstract var valueParameters: List<IrValueParameter>

    /**
     * The first `contextReceiverParametersCount` value parameters are context receivers
     */
    abstract var contextReceiverParametersCount: Int

    abstract var body: IrBody?

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
        visitor.visitFunction(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        super<IrTypeParametersContainer>.acceptChildren(visitor, data)

        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }

        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        super<IrTypeParametersContainer>.transformChildren(transformer, data)

        dispatchReceiverParameter = dispatchReceiverParameter?.transform(transformer, data)
        extensionReceiverParameter = extensionReceiverParameter?.transform(transformer, data)
        valueParameters = valueParameters.transformIfNeeded(transformer, data)

        body = body?.transform(transformer, data)
    }
}
