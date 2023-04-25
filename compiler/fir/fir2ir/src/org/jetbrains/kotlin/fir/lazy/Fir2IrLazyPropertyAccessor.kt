/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.isFacadeClass
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
    firParentClass: FirRegularClass?,
    symbol: IrSimpleFunctionSymbol,
    isFakeOverride: Boolean
) : AbstractFir2IrLazyFunction<FirCallableDeclaration>(components, startOffset, endOffset, origin, symbol, isFakeOverride) {
    init {
        symbol.bind(this)
    }

    override val fir: FirCallableDeclaration
        get() = firAccessor ?: firParentProperty

    // TODO: investigate why some deserialized properties are inline
    override var isInline: Boolean
        get() = firAccessor?.isInline == true
        set(_) = mutationNotSupported()

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override var name: Name
        get() = Name.special("<${if (isSetter) "set" else "get"}-${firParentProperty.name}>")
        set(_) = mutationNotSupported()

    override var returnType: IrType by lazyVar(lock) {
        if (isSetter) irBuiltIns.unitType else firParentProperty.returnTypeRef.toIrType(typeConverter, conversionTypeContext)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar(lock) {
        val containingClass = (parent as? IrClass)?.takeUnless { it.isFacadeClass }
        if (containingClass != null && shouldHaveDispatchReceiver(containingClass)) {
            createThisReceiverParameter(thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"))
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar(lock) {
        firParentProperty.receiverParameter?.let {
            createThisReceiverParameter(it.typeRef.toIrType(typeConverter, conversionTypeContext), it)
        }
    }

    override var contextReceiverParametersCount: Int = fir.contextReceiversForFunctionOrContainingProperty().size

    override var valueParameters: List<IrValueParameter> by lazyVar(lock) {
        if (!isSetter && contextReceiverParametersCount == 0) emptyList()
        else {
            declarationStorage.enterScope(this)

            buildList {
                declarationStorage.addContextReceiverParametersTo(
                    fir.contextReceiversForFunctionOrContainingProperty(),
                    this@Fir2IrLazyPropertyAccessor,
                    this@buildList,
                )

                if (isSetter) {
                    val valueParameter = firAccessor?.valueParameters?.firstOrNull()
                    add(
                        declarationStorage.createDefaultSetterParameter(
                            startOffset, endOffset,
                            (valueParameter?.returnTypeRef ?: firParentProperty.returnTypeRef).toIrType(
                                typeConverter, conversionTypeContext
                            ),
                            parent = this@Fir2IrLazyPropertyAccessor,
                            firValueParameter = valueParameter,
                            name = valueParameter?.name,
                            isCrossinline = valueParameter?.isCrossinline == true,
                            isNoinline = valueParameter?.isNoinline == true
                        )
                    )
                }
            }.apply {
                declarationStorage.leaveScope(this@Fir2IrLazyPropertyAccessor)
            }
        }
    }

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by lazyVar(lock) {
        if (firParentClass == null) return@lazyVar emptyList()
        firParentProperty.generateOverriddenAccessorSymbols(firParentClass, !isSetter)
    }

    override val initialSignatureFunction: IrFunction? by lazy {
        (fir as? FirSyntheticPropertyAccessor)?.delegate?.let { declarationStorage.getIrFunctionSymbol(it.symbol).owner }
    }

    override val containerSource: DeserializedContainerSource?
        get() = firParentProperty.containerSource

    private val conversionTypeContext = if (isSetter) ConversionTypeContext.IN_SETTER else ConversionTypeContext.DEFAULT
}
