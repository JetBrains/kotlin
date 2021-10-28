/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

interface IrLazyFunctionBase : IrLazyDeclarationBase, IrTypeParametersContainer {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: FunctionDescriptor

    val initialSignatureFunction: IrFunction?

    fun createInitialSignatureFunction(): Lazy<IrFunction?> =
        // Need SYNCHRONIZED; otherwise two stubs generated in parallel may fight for the same symbol.
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            descriptor.initialSignatureDescriptor?.takeIf { it != descriptor }?.original?.let(stubGenerator::generateFunctionStub)
        }

    fun createValueParameters(): List<IrValueParameter> =
        typeTranslator.buildWithScope(this) {
            val result = arrayListOf<IrValueParameter>()
            descriptor.contextReceiverParameters.mapIndexedTo(result) { i, contextReceiverParameter ->
                factory.createValueParameter(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrValueParameterSymbolImpl(contextReceiverParameter),
                    Name.identifier("contextReceiverParameter$i"), i, contextReceiverParameter.type.toIrType(),
                    null, isCrossinline = false, isNoinline = false, isHidden = false, isAssignable = false
                ).apply { parent = this@IrLazyFunctionBase }
            }
            descriptor.valueParameters.mapTo(result) {
                stubGenerator.generateValueParameterStub(it, it.index + descriptor.contextReceiverParameters.size)
                    .apply { parent = this@IrLazyFunctionBase }
            }
        }

    fun createReceiverParameter(
        parameter: ReceiverParameterDescriptor?,
        functionDispatchReceiver: Boolean = false,
    ): IrValueParameter? =
        if (functionDispatchReceiver && stubGenerator.extensions.isStaticFunction(descriptor)) null
        else typeTranslator.buildWithScope(this) {
            parameter?.generateReceiverParameterStub()?.also { it.parent = this@IrLazyFunctionBase }
        }

    fun createReturnType(): IrType =
        typeTranslator.buildWithScope(this) {
            descriptor.returnType!!.toIrType()
        }
}
