/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.descriptors

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.IrType

@ObsoleteDescriptorBasedAPI
@K1Deprecation
fun IrDelegatingConstructorCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
): IrDelegatingConstructorCallImpl {
    val descriptor = symbol.descriptor
    return IrDelegatingConstructorCallImplWithShape(
        startOffset, endOffset, type, symbol,
        typeArgumentsCount = descriptor.typeParametersCount,
        valueArgumentsCount = descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
        contextParameterCount = descriptor.contextReceiverParameters.size,
        hasDispatchReceiver = descriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
    )
}
