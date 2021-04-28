/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.backend.common.ir.isOverridableOrOverrides
import org.jetbrains.kotlin.backend.wasm.lower.WasmSignature
import org.jetbrains.kotlin.backend.wasm.lower.wasmSignature
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isInterface

class ClassMetadata(
    val klass: IrClass,
    val superClass: ClassMetadata?,
    irBuiltIns: IrBuiltIns
) {
    // List of all fields including fields of super classes
    // In Wasm order
    val fields: List<IrField> =
        superClass?.fields.orEmpty() + klass.declarations.filterIsInstance<IrField>()

    // Implemented interfaces in no particular order
    val interfaces: List<IrClass> = klass.allSuperInterfaces()

    // Virtual methods in Wasm order
    // TODO: Collect interface methods separately
    val virtualMethods: List<VirtualMethodMetadata> = run {
        val virtualFunctions =
            klass.declarations
                .filterVirtualFunctions()
                .map {
                    VirtualMethodMetadata(
                        it,
                        it.wasmSignature(irBuiltIns)
                    )
                }

        val signatureToVirtualFunction = virtualFunctions.associateBy { it.signature }

        val superSignatures = superClass?.virtualMethodsSignatures.orEmpty()

        val newVirtualMethods = virtualFunctions.filter { it.signature !in superSignatures }
        val superVirtualMethods = superClass?.virtualMethods.orEmpty().map {
            signatureToVirtualFunction[it.signature] ?: it
        }
        val orderedVirtualFunctions = superVirtualMethods + newVirtualMethods

        orderedVirtualFunctions
    }

    init {
        val signatureToFunctions = mutableMapOf<WasmSignature, MutableList<IrSimpleFunction>>()
        for (vm in virtualMethods) {
            signatureToFunctions.getOrPut(vm.signature) { mutableListOf() }.add(vm.function)
        }

        for ((sig, functions) in signatureToFunctions) {
            if (functions.size > 1) {
                val funcList = functions.joinToString { " ---- ${it.fqNameWhenAvailable} \n" }
                // TODO: Check in FE
                error(
                    "Class ${klass.fqNameWhenAvailable} has ${functions.size} methods with the same signature $sig\n $funcList"
                )
            }
        }
    }

    private val virtualMethodsSignatures: Set<WasmSignature> =
        virtualMethods.map { it.signature }.toSet()
}

class InterfaceMetadata(
    val iface: IrClass,
    irBuiltIns: IrBuiltIns
) {
    val methods: List<VirtualMethodMetadata> =
        iface.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { !it.isFakeOverride && it.visibility != DescriptorVisibilities.PRIVATE && it.modality != Modality.FINAL }
            .map {
                VirtualMethodMetadata(
                    it,
                    it.wasmSignature(irBuiltIns)
                )
            }
}


class VirtualMethodMetadata(
    val function: IrSimpleFunction,
    val signature: WasmSignature
)

fun IrClass.allSuperInterfaces(): List<IrClass> =
    superTypes.map {
        it.classifierOrFail.owner as IrClass
    }.flatMap {
        (if (it.isInterface) listOf(it) else emptyList()) + it.allSuperInterfaces()
    }

fun List<IrDeclaration>.filterVirtualFunctions(): List<IrSimpleFunction> =
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
