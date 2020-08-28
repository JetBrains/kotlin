/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrType
import kotlin.properties.ReadWriteProperty

interface IrLazyFunctionBase : IrLazyDeclarationBase, IrTypeParametersContainer {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: FunctionDescriptor

    val initialSignatureFunction: IrFunction?

    fun createInitialSignatureFunction(): Lazy<IrFunction?> =
        lazy(LazyThreadSafetyMode.PUBLICATION) {
            descriptor.initialSignatureDescriptor?.takeIf { it != descriptor }?.original?.let(stubGenerator::generateFunctionStub)
        }

    fun createValueParameters(): ReadWriteProperty<Any?, List<IrValueParameter>> =
        lazyVar {
            typeTranslator.buildWithScope(this) {
                descriptor.valueParameters.mapTo(arrayListOf()) {
                    stubGenerator.generateValueParameterStub(it).apply { parent = this@IrLazyFunctionBase }
                }
            }
        }

    fun createReceiverParameter(parameter: ReceiverParameterDescriptor?): ReadWriteProperty<Any?, IrValueParameter?> =
        lazyVar {
            typeTranslator.buildWithScope(this) {
                parameter?.generateReceiverParameterStub()?.also { it.parent = this@IrLazyFunctionBase }
            }
        }

    fun createReturnType(): ReadWriteProperty<Any?, IrType> =
        lazyVar {
            typeTranslator.buildWithScope(this) {
                descriptor.returnType!!.toIrType()
            }
        }
}
