/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi2ir.lazy

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyFunctionBase
import org.jetbrains.kotlin.ir.types.IrType

interface Psi2IrLazyFunctionBase : IrLazyFunctionBase, Psi2IrLazyDeclarationBase {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override val descriptor: FunctionDescriptor

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