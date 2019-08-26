/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.codegen

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.ir.backend.js.utils.Signature
import org.jetbrains.kotlin.ir.backend.js.utils.jsFunctionSignature
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.isInterface

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
                .map { VirtualMethodMetadata(it, jsFunctionSignature(it)) }

        val signatureToVirtualFunction = virtualFunctions.associateBy { it.signature }

        val newVirtualMethods = virtualFunctions.filter { it.signature !in superClass?.virtualMethodsSignatures.orEmpty() }
        val superVirtualMethods = superClass?.virtualMethods.orEmpty().map { signatureToVirtualFunction.getValue(it.signature) }
        val orderedVirtualFunctions = superVirtualMethods + newVirtualMethods

        orderedVirtualFunctions
    }

    val virtualMethodsSignatures: Set<Signature> =
        virtualMethods.map { it.signature }.toHashSet()
}

class VirtualMethodMetadata(
    val function: IrSimpleFunction,
    val signature: Signature
)

private val IrClass.superBroadClasses: List<IrClass>
    get() = superTypes.map { it.classifierOrFail.owner as IrClass }

fun IrClass.allInterfaces(): List<IrClass> {
    val shallowSuperClasses = superBroadClasses
    return shallowSuperClasses.filter { it.isInterface } + shallowSuperClasses.flatMap { it.allInterfaces() }
}

fun List<IrFunction>.filterVirtualFunctions(): List<IrSimpleFunction> =
    asSequence()
        .filterIsInstance<IrSimpleFunction>()
        .filter { it.dispatchReceiverParameter != null }
        .map { it.realOverrideTarget }
        .filter { it.isOverridableOrOverrides }
        .distinct()
        .toList()

fun IrClass.getSuperClass(builtIns: IrBuiltIns): IrClass? =
    when (this) {
        builtIns.anyClass.owner -> null
        else -> {
            superTypes
                .map { it.classifierOrFail.owner as IrClass }
                .singleOrNull { !it.isInterface } ?: builtIns.anyClass.owner
        }
    }

fun IrClass.allFields(builtIns: IrBuiltIns): List<IrField> =
    getSuperClass(builtIns)?.allFields(builtIns).orEmpty() + declarations.filterIsInstance<IrField>()
