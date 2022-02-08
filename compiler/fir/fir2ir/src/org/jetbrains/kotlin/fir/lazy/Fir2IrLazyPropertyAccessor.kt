/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.backend.ConversionTypeContext
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.generateOverriddenAccessorSymbols
import org.jetbrains.kotlin.fir.backend.toIrType
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyPropertyAccessor(
    components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    private val firAccessor: FirPropertyAccessor?,
    private val isSetter: Boolean,
    private val firParentProperty: FirProperty,
    firParentClass: FirRegularClass,
    symbol: Fir2IrSimpleFunctionSymbol,
    isFakeOverride: Boolean
) : AbstractFir2IrLazyFunction<FirCallableDeclaration>(components, startOffset, endOffset, origin, symbol, isFakeOverride) {
    init {
        symbol.bind(this)
    }

    override val fir: FirCallableDeclaration
        get() = firAccessor ?: firParentProperty

    // TODO: investigate why some deserialized properties are inline
    override val isInline: Boolean
        get() = firAccessor?.isInline == true

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override val name: Name
        get() = Name.special("<${if (isSetter) "set" else "get"}-${firParentProperty.name}>")

    override var returnType: IrType by lazyVar(lock) {
        if (isSetter) irBuiltIns.unitType else firParentProperty.returnTypeRef.toIrType(typeConverter, conversionTypeContext)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar(lock) {
        val containingClass = parent as? IrClass
        if (containingClass != null && shouldHaveDispatchReceiver(containingClass, firParentProperty)
        ) {
            createThisReceiverParameter(thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"))
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar(lock) {
        firParentProperty.receiverTypeRef?.let {
            createThisReceiverParameter(it.toIrType(typeConverter, conversionTypeContext))
        }
    }

    override var contextReceiverParametersCount: Int = 0

    override var valueParameters: List<IrValueParameter> by lazyVar(lock) {
        if (!isSetter) emptyList()
        else {
            declarationStorage.enterScope(this)
            listOf(
                declarationStorage.createDefaultSetterParameter(
                    startOffset, endOffset,
                    (firAccessor?.valueParameters?.firstOrNull()?.returnTypeRef ?: firParentProperty.returnTypeRef).toIrType(
                        typeConverter, conversionTypeContext
                    ),
                    parent = this@Fir2IrLazyPropertyAccessor
                )
            ).apply {
                declarationStorage.leaveScope(this@Fir2IrLazyPropertyAccessor)
            }
        }
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by lazyVar(lock) {
        firParentProperty.generateOverriddenAccessorSymbols(
            firParentClass,
            !isSetter,
            session,
            scopeSession,
            declarationStorage,
            fakeOverrideGenerator
        )
    }

    override val initialSignatureFunction: IrFunction? by lazy {
        (fir as? FirSyntheticPropertyAccessor)?.delegate?.let { declarationStorage.getIrFunctionSymbol(it.symbol).owner }
    }

    override val containerSource: DeserializedContainerSource?
        get() = firParentProperty.containerSource

    private val conversionTypeContext = if (isSetter) ConversionTypeContext.DEFAULT.inSetter() else ConversionTypeContext.DEFAULT
}
