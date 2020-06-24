/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.Fir2IrConstructorSymbol
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.mapOptimized
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.name.Name

class Fir2IrLazyConstructor(
    components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    fir: FirConstructor,
    symbol: Fir2IrConstructorSymbol
) : AbstractFir2IrLazyDeclaration<FirConstructor, IrConstructor>(
    components, startOffset, endOffset, origin, fir, symbol
), IrConstructor {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override val isPrimary: Boolean
        get() = fir.isPrimary

    @ObsoleteDescriptorBasedAPI
    override val descriptor: ClassConstructorDescriptor
        get() = super.descriptor as ClassConstructorDescriptor

    override val symbol: Fir2IrConstructorSymbol
        get() = super.symbol as Fir2IrConstructorSymbol

    override val isInline: Boolean
        get() = fir.isInline

    override val isExternal: Boolean
        get() = fir.isExternal

    override val isExpect: Boolean
        get() = fir.isExpect

    override var body: IrBody? = null

    override val name: Name
        get() = Name.special("<init>")

    override var visibility: Visibility
        get() = fir.visibility
        set(_) {
            throw AssertionError("Mutating Fir2Ir lazy elements is not possible")
        }

    override var returnType: IrType by lazyVar {
        fir.returnTypeRef.toIrType(typeConverter)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar {
        val containingClass = parent as? IrClass
        val outerClass = containingClass?.parentClassOrNull
        if (containingClass?.isInner == true && outerClass != null) {
            declarationStorage.enterScope(this)
            declareThisReceiverParameter(
                symbolTable,
                thisType = outerClass.thisReceiver!!.type,
                thisOrigin = origin
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazyConstructor)
            }
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter?
        get() = null
        set(_) {
            throw AssertionError("Mutating Fir2Ir lazy elements is not possible")
        }

    override var valueParameters: List<IrValueParameter> by lazyVar {
        declarationStorage.enterScope(this)
        fir.valueParameters.mapIndexed { index, valueParameter ->
            declarationStorage.createIrParameter(
                valueParameter, index,
                useStubForDefaultValueStub = (parent as? IrClass)?.name != Name.identifier("Enum")
            ).apply {
                this.parent = this@Fir2IrLazyConstructor
            }
        }.apply {
            declarationStorage.leaveScope(this@Fir2IrLazyConstructor)
        }
    }

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R {
        return visitor.visitConstructor(this, data)
    }

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        typeParameters.forEach { it.accept(visitor, data) }

        dispatchReceiverParameter?.accept(visitor, data)
        extensionReceiverParameter?.accept(visitor, data)
        valueParameters.forEach { it.accept(visitor, data) }

        body?.accept(visitor, data)
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        typeParameters = typeParameters.mapOptimized { it.transform(transformer, data) }

        dispatchReceiverParameter = dispatchReceiverParameter?.transform(transformer, data)
        extensionReceiverParameter = extensionReceiverParameter?.transform(transformer, data)
        valueParameters = valueParameters.mapOptimized { it.transform(transformer, data) }

        body = body?.transform(transformer, data)
    }
}