/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.ir.backend.js.utils.Signature
import org.jetbrains.kotlin.ir.backend.js.utils.functionSignature
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol

class ClassMetadata(
    val klass: IrClass,
    val superClass: ClassMetadata?
) {
    // List of all fields including fields of super classes
    // In Wasm order
    val fields: List<IrField> =
        superClass?.fields.orEmpty() + klass.declarations.filterIsInstance<IrField>()

    // Implemented interfaces in no particular order
    val interfaces: List<IrClass> = klass.allInterfaces()

    // Virtual methods in Wasm order
    val virtualMethods: List<VirtualMethodMetadata> = run {
        val virtualFunctions =
            klass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .filterVirtualFunctions()
                .map { VirtualMethodMetadata(it, functionSignature(it)) }

        val signatureToVirtualFunction = virtualFunctions.associateBy { it.signature }

        val newVirtualMethods = virtualFunctions.filter { it.signature !in superClass?.virtualMethodsSignatures.orEmpty() }
        val superVirtualMethods = superClass?.virtualMethods.orEmpty().map { signatureToVirtualFunction.getValue(it.signature) }
        val orderedVirtualFunctions = superVirtualMethods + newVirtualMethods

        orderedVirtualFunctions
    }

    val virtualMethodsSignatures: Set<Signature> =
        virtualMethods.map { it.signature }.toHashSet()
}

class InterfaceMetadata(
    val id: Int
)

class VirtualMethodMetadata(
    val function: IrSimpleFunction,
    val signature: Signature
)