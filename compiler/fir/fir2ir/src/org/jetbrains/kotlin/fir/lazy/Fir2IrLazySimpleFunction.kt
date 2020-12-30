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

class Fir2IrLazySimpleFunction(
    components: Fir2IrComponents,
    startOffset: Int,
    endOffset: Int,
    origin: IrDeclarationOrigin,
    override val fir: FirSimpleFunction,
    firParent: FirRegularClass,
    symbol: Fir2IrSimpleFunctionSymbol,
    isFakeOverride: Boolean
) : AbstractFir2IrLazyFunction<FirSimpleFunction>(components, startOffset, endOffset, origin, symbol, isFakeOverride) {
    init {
        symbol.bind(this)
        classifierStorage.preCacheTypeParameters(fir)
    }

    override var annotations: List<IrConstructorCall> by createLazyAnnotations()

    override val name: Name
        get() = fir.name

    override var returnType: IrType by lazyVar {
        fir.returnTypeRef.toIrType(typeConverter)
    }

    override var dispatchReceiverParameter: IrValueParameter? by lazyVar {
        val containingClass = parent as? IrClass
        if (containingClass != null && shouldHaveDispatchReceiver(containingClass, fir)) {
            createThisReceiverParameter(thisType = containingClass.thisReceiver?.type ?: error("No this receiver for containing class"))
        } else null
    }

    override var extensionReceiverParameter: IrValueParameter? by lazyVar {
        fir.receiverTypeRef?.let {
            createThisReceiverParameter(it.toIrType(typeConverter))
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
        val parent = parent
        if (isFakeOverride && parent is Fir2IrLazyClass) {
            parent.declarations
            fakeOverrideGenerator.getOverriddenSymbols(this)?.let { return@lazyVar it }
        }
        fir.generateOverriddenFunctionSymbols(firParent, session, scopeSession, declarationStorage)
    }

    override val containerSource: DeserializedContainerSource?
        get() = fir.containerSource
}
