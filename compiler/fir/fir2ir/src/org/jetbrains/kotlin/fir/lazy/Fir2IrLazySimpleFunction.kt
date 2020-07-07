/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.findMatchingOverriddenSymbolsFromSupertypes
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.transformIfNeeded
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class Fir2IrLazySimpleFunction(
    components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    fir: FirSimpleFunction,
    symbol: Fir2IrSimpleFunctionSymbol,
    override val isFakeOverride: Boolean
) : AbstractFir2IrLazyDeclaration<FirSimpleFunction, IrSimpleFunction>(
    components, startOffset, endOffset, origin, fir, symbol
), IrSimpleFunction {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override val isTailrec: Boolean
        get() = fir.isTailRec

    override val isSuspend: Boolean
        get() = fir.isSuspend

    override val isOperator: Boolean
        get() = fir.isOperator

    @ObsoleteDescriptorBasedAPI
    override val descriptor: FunctionDescriptor
        get() = super.descriptor as FunctionDescriptor

    override val symbol: Fir2IrSimpleFunctionSymbol
        get() = super.symbol as Fir2IrSimpleFunctionSymbol

    override val isInline: Boolean
        get() = fir.isInline

    override val isExternal: Boolean
        get() = fir.isExternal

    override val isExpect: Boolean
        get() = fir.isExpect

    override var body: IrBody? = null

    override val name: Name
        get() = fir.name

    override var visibility: Visibility
        get() = fir.visibility
        set(_) {
            error("Mutating Fir2Ir lazy elements is not possible")
        }

    override val modality: Modality
        get() = fir.modality!!

    override var correspondingPropertySymbol: IrPropertySymbol? = null

    override var attributeOwnerId: IrAttributeContainer = this

    override var returnType: IrType by lazyVar {
        fir.returnTypeRef.toIrType(typeConverter)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar {
        val containingClass = parent as? IrClass
        if (!fir.isStatic && containingClass != null) {
            declarationStorage.enterScope(this)
            declareThisReceiverParameter(
                symbolTable,
                thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"),
                thisOrigin = origin
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction)
            }
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar {
        fir.receiverTypeRef?.let {
            declarationStorage.enterScope(this)
            declareThisReceiverParameter(
                symbolTable,
                thisType = it.toIrType(typeConverter),
                thisOrigin = origin,
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction)
            }
        }
    }

    override var valueParameters: List<IrValueParameter> by lazyVar {
        declarationStorage.enterScope(this)
        fir.valueParameters.mapIndexed { index, valueParameter ->
            declarationStorage.createIrParameter(
                valueParameter, index,
            ).apply {
                this.parent = this@Fir2IrLazySimpleFunction
            }
        }.apply {
            declarationStorage.leaveScope(this@Fir2IrLazySimpleFunction)
        }
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by lazyVar {
        val containingClass = parent as? IrClass
        containingClass?.findMatchingOverriddenSymbolsFromSupertypes(irBuiltIns, this)
            ?.filterIsInstance<IrSimpleFunctionSymbol>().orEmpty()
    }

    override var metadata: MetadataSource?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitSimpleFunction(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }

        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }

        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters = typeParameters.transformIfNeeded(transformer, data)

        dispatchReceiverParameter = dispatchReceiverParameter?.transform(transformer, data)
        extensionReceiverParameter = extensionReceiverParameter?.transform(transformer, data)
        valueParameters = valueParameters.transformIfNeeded(transformer, data)

        body = body?.transform(transformer, data)
    }
}
