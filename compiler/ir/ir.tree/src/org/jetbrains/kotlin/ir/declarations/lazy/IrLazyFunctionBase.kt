/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.name.Name

interface IrLazyFunctionBase : IrLazyDeclarationBase, IrTypeParametersContainer {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: FunctionDescriptor

    val initialSignatureFunction: IrFunction?

    fun getTopLevelDeclaration(): IrDeclaration {
        var current: IrDeclaration = this
        while (current.parent !is IrPackageFragment) {
            current = current.parent as IrDeclaration
        }
        return current
    }

    fun createInitialSignatureFunction(): Lazy<IrFunction?> =
        // Need SYNCHRONIZED; otherwise two stubs generated in parallel may fight for the same symbol.
        lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            val initialSignatureDescriptor = descriptor.initialSignatureDescriptor
                ?: return@lazy null
            if (initialSignatureDescriptor == descriptor)
                return@lazy null
            stubGenerator.generateFunctionStub(initialSignatureDescriptor.original)
        }

    fun createReturnType(): IrType =
        typeTranslator.buildWithScope(this) {
            descriptor.returnType!!.toIrType()
        }
}
