/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.ir.backend.js.utils.Signature
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol

class ClassMetadata(
    val ir: IrClass,

    // Class ID is a pointer to class RTTI in linear memory
    val id: Int,

    // null for kotlin.Any class
    val superClass: ClassMetadata?,

    // List of all fields including fields of super classes
    // In Wasm order
    val fields: List<IrFieldSymbol>,

    // Implemented interfaces in no particular order
    val interfaces: List<InterfaceMetadata>,

    // Virtual methods in Wasm order
    val virtualMethods: List<VirtualMethodMetadata>
) {
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