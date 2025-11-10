/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.descriptors

import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrEnumConstructorCallImplWithShape
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImplWithShape
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType

@ObsoleteDescriptorBasedAPI
fun IrCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrSimpleFunctionSymbol,
    origin: IrStatementOrigin? = null,
    superQualifierSymbol: IrClassSymbol? = null,
): IrCallImpl {
    val descriptor = symbol.descriptor
    return IrCallImplWithShape(
        startOffset, endOffset, type, symbol,
        typeArgumentsCount = descriptor.typeParametersCount,
        valueArgumentsCount = descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
        contextParameterCount = descriptor.contextReceiverParameters.size,
        hasDispatchReceiver = descriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
        origin = origin,
        superQualifierSymbol = superQualifierSymbol,
    )
}

@ObsoleteDescriptorBasedAPI
fun IrAnnotationImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    constructorSymbol: IrConstructorSymbol,
    origin: IrStatementOrigin? = null,
): IrAnnotationImpl {
    val constructorDescriptor = constructorSymbol.descriptor
    val classTypeParametersCount = constructorDescriptor.constructedClass.original.declaredTypeParameters.size
    val totalTypeParametersCount = constructorDescriptor.typeParameters.size
    val valueParametersCount = constructorDescriptor.valueParameters.size + constructorDescriptor.contextReceiverParameters.size
    return IrAnnotationImplWithShape(
        startOffset, endOffset,
        type,
        constructorSymbol,
        typeArgumentsCount = totalTypeParametersCount,
        constructorTypeArgumentsCount = totalTypeParametersCount - classTypeParametersCount,
        valueArgumentsCount = valueParametersCount,
        contextParameterCount = constructorDescriptor.contextReceiverParameters.size,
        hasDispatchReceiver = constructorDescriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = constructorDescriptor.extensionReceiverParameter != null,
        origin = origin,
    )
}

@ObsoleteDescriptorBasedAPI
fun IrConstructorCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    constructorSymbol: IrConstructorSymbol,
    origin: IrStatementOrigin? = null,
): IrConstructorCallImpl {
    val constructorDescriptor = constructorSymbol.descriptor
    val classTypeParametersCount = constructorDescriptor.constructedClass.original.declaredTypeParameters.size
    val totalTypeParametersCount = constructorDescriptor.typeParameters.size
    val valueParametersCount = constructorDescriptor.valueParameters.size + constructorDescriptor.contextReceiverParameters.size
    return IrConstructorCallImplWithShape(
        startOffset, endOffset,
        type,
        constructorSymbol,
        typeArgumentsCount = totalTypeParametersCount,
        constructorTypeArgumentsCount = totalTypeParametersCount - classTypeParametersCount,
        valueArgumentsCount = valueParametersCount,
        contextParameterCount = constructorDescriptor.contextReceiverParameters.size,
        hasDispatchReceiver = constructorDescriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = constructorDescriptor.extensionReceiverParameter != null,
        origin = origin,
    )
}

@ObsoleteDescriptorBasedAPI
fun IrEnumConstructorCallImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrConstructorSymbol,
    typeArgumentsCount: Int,
): IrEnumConstructorCallImpl {
    val descriptor = symbol.descriptor
    return IrEnumConstructorCallImplWithShape(
        startOffset, endOffset, type, symbol,
        typeArgumentsCount = typeArgumentsCount,
        valueArgumentsCount = descriptor.valueParameters.size + descriptor.contextReceiverParameters.size,
        contextParameterCount = descriptor.contextReceiverParameters.size,
        hasDispatchReceiver = descriptor.dispatchReceiverParameter != null,
        hasExtensionReceiver = descriptor.extensionReceiverParameter != null,
    )
}

@ObsoleteDescriptorBasedAPI
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

@ObsoleteDescriptorBasedAPI
fun IrFunctionReferenceImpl.Companion.fromSymbolDescriptor(
    startOffset: Int,
    endOffset: Int,
    type: IrType,
    symbol: IrFunctionSymbol,
    reflectionTarget: IrFunctionSymbol?,
    origin: IrStatementOrigin? = null,
): IrFunctionReferenceImpl = IrFunctionReferenceImplWithShape(
    startOffset = startOffset, endOffset = endOffset,
    type = type,
    symbol = symbol,
    typeArgumentsCount = symbol.descriptor.typeParametersCount,
    valueArgumentsCount = symbol.descriptor.valueParameters.size + symbol.descriptor.contextReceiverParameters.size,
    contextParameterCount = symbol.descriptor.contextReceiverParameters.size,
    hasDispatchReceiver = symbol.descriptor.dispatchReceiverParameter != null,
    hasExtensionReceiver = symbol.descriptor.extensionReceiverParameter != null,
    reflectionTarget = reflectionTarget,
    origin = origin
)