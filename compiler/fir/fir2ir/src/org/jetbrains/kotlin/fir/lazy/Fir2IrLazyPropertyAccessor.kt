/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lazy

import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.symbols.Fir2IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.lazyVar
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class Fir2IrLazyPropertyAccessor(
    components: Fir2IrComponents,
    override val startOffset: Int,
    override val endOffset: Int,
    override var origin: IrDeclarationOrigin,
    private val firAccessor: FirPropertyAccessor?,
    private val isSetter: Boolean,
    private val firParentProperty: FirProperty,
    firParentClass: FirRegularClass,
    override val symbol: Fir2IrSimpleFunctionSymbol,
    override val isFakeOverride: Boolean
) : AbstractFir2IrLazyFunction<FirMemberDeclaration>(components, startOffset, endOffset, origin, symbol, isFakeOverride) {
    init {
        symbol.bind(this)
    }

    override val fir: FirMemberDeclaration
        get() = firAccessor ?: firParentProperty

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override val name: Name
        get() = Name.special("<${if (isSetter) "set" else "get"}-${firParentProperty.name}>")

    override var returnType: IrType by lazyVar {
        if (isSetter) irBuiltIns.unitType else firParentProperty.returnTypeRef.toIrType(typeConverter, conversionTypeContext)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar {
        val containingClass = parent as? IrClass
        if (containingClass != null && shouldHaveDispatchReceiver(containingClass, firParentProperty)
        ) {
            createThisReceiverParameter(thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"))
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar {
        firParentProperty.receiverTypeRef?.let {
            createThisReceiverParameter(it.toIrType(typeConverter, conversionTypeContext))
        }
    }

    override var valueParameters: List<IrValueParameter> by lazyVar {
        if (!isSetter) emptyList()
        else {
            declarationStorage.enterScope(this)
            listOf(
                declarationStorage.createDefaultSetterParameter(
                    startOffset, endOffset, origin,
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

    override var overriddenSymbols: List<IrSimpleFunctionSymbol> by lazyVar {
        firParentProperty.generateOverriddenAccessorSymbols(firParentClass, !isSetter, session, scopeSession, declarationStorage)
    }

    override val containerSource: DeserializedContainerSource?
        get() = firParentProperty.containerSource

    private val conversionTypeContext = if (isSetter) ConversionTypeContext.DEFAULT.inSetter() else ConversionTypeContext.DEFAULT
}