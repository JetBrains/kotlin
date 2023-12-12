/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.ir2wasm

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.js.utils.erasedUpperBound
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name

data class WasmSignature(
    val name: Name,
    val extensionReceiverType: IrType?,
    val valueParametersType: List<IrType>,
    val returnType: IrType,
    // Needed for bridges to final non-override methods
    // that indirectly implement interfaces. For example:
    //    interface I { fun foo() }
    //    class C1 { fun foo() {} }
    //    class C2 : C1(), I
    val isVirtual: Boolean,
) {
    override fun toString(): String {
        val er = extensionReceiverType?.let { "(er: ${it.render()}) " } ?: ""
        val parameters = valueParametersType.joinToString(", ") { it.render() }
        val nonVirtual = if (!isVirtual) "(non-virtual) " else ""
        return "[$nonVirtual$er$name($parameters) -> ${returnType.render()}]"
    }
}

fun IrSimpleFunction.wasmSignature(irBuiltIns: IrBuiltIns): WasmSignature =
    WasmSignature(
        name,
        extensionReceiverParameter?.type?.toWasmSignatureType(irBuiltIns),
        valueParameters.map { it.type.toWasmSignatureType(irBuiltIns) },
        returnType.toWasmSignatureType(irBuiltIns),
        isOverridableOrOverrides
    )

private fun IrType.toWasmSignatureType(irBuiltIns: IrBuiltIns): IrType =
    when (this) {
        is IrSimpleType -> toWasmSignatureSimpleType(irBuiltIns)
        else -> this
    }

private fun IrSimpleType.toWasmSignatureSimpleType(irBuiltIns: IrBuiltIns): IrSimpleType {
    // Special case: Kotlin allows overloading functions based on Array type arguments
    if (this.classifier == irBuiltIns.arrayClass) {
        return irBuiltIns.arrayClass.createType(
            hasQuestionMark = isNullable(),
            arguments = arguments.map { it.toWasmSignatureTypeArgument(irBuiltIns) }
        )
    }

    val klass = (this.erasedUpperBound?.symbol ?: irBuiltIns.anyClass).owner
    return klass.defaultType.withNullability(this.isNullable())
}

private fun IrTypeArgument.toWasmSignatureTypeArgument(irBuiltIns: IrBuiltIns): IrTypeArgument {
    return when (this) {
        is IrStarProjection -> this
        is IrTypeProjection -> {
            when (val type = type) {
                is IrSimpleType -> type.toWasmSignatureSimpleType(irBuiltIns)
                else -> this
            }
        }
    }
}

class VirtualMethodMetadata(
    val function: IrSimpleFunction,
    val signature: WasmSignature
)

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
        val virtualFunctions = klass.declarations
            .asSequence()
            .filterVirtualFunctions()
            .mapTo(mutableListOf()) { VirtualMethodMetadata(it, it.wasmSignature(irBuiltIns)) }

        val superClassVirtualMethods = superClass?.virtualMethods
        if (superClassVirtualMethods.isNullOrEmpty()) return@run virtualFunctions

        val result = mutableListOf<VirtualMethodMetadata>()

        val signatureToVirtualFunction = virtualFunctions.associateBy { it.signature }
        superClassVirtualMethods.mapTo(result) { signatureToVirtualFunction[it.signature] ?: it }

        val superSignatures = superClassVirtualMethods.mapTo(mutableSetOf()) { it.signature }
        virtualFunctions.filterTo(result) { it.signature !in superSignatures }

        result
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
                error("Class ${klass.fqNameWhenAvailable} has ${functions.size} methods with the same signature $sig\n $funcList")
            }
        }
    }
}

class InterfaceMetadata(val iFace: IrClass, irBuiltIns: IrBuiltIns) {
    val methods: List<VirtualMethodMetadata> = iFace.declarations
        .asSequence()
        .filterIsInstance<IrSimpleFunction>()
        .filter { !it.isFakeOverride && it.visibility != DescriptorVisibilities.PRIVATE && it.modality != Modality.FINAL }
        .mapTo(mutableListOf()) { VirtualMethodMetadata(it, it.wasmSignature(irBuiltIns)) }
}

fun IrClass.allSuperInterfaces(): List<IrClass> {
    fun allSuperInterfacesImpl(currentClass: IrClass, result: MutableList<IrClass>) {
        for (superType in currentClass.superTypes) {
            allSuperInterfacesImpl(superType.classifierOrFail.owner as IrClass, result)
        }
        if (currentClass.isInterface) result.add(currentClass)
    }

    return mutableListOf<IrClass>().also {
        allSuperInterfacesImpl(this, it)
    }
}

fun Sequence<IrDeclaration>.filterVirtualFunctions(): Sequence<IrSimpleFunction> =
    this.filterIsInstance<IrSimpleFunction>()
        .filter { it.dispatchReceiverParameter != null }
        .map { it.realOverrideTarget }
        .filter { it.isOverridableOrOverrides }
        .distinct()

fun IrClass.getSuperClass(builtIns: IrBuiltIns): IrClass? =
    when (this) {
        builtIns.anyClass.owner -> null
        else -> superTypes
            .map { it.classifierOrFail.owner as IrClass }
            .singleOrNull { !it.isInterface } ?: builtIns.anyClass.owner
    }

fun IrClass.allFields(builtIns: IrBuiltIns): List<IrField> =
    getSuperClass(builtIns)?.allFields(builtIns).orEmpty() + declarations.filterIsInstance<IrField>()

fun IrClass.hasInterfaceSuperClass(): Boolean {
    var superClass: IrClass? = null
    for (superType in superTypes) {
        val typeAsClass = superType.classifierOrFail.owner as IrClass
        if (typeAsClass.isInterface) {
            return true
        } else {
            superClass = typeAsClass
        }
    }
    return superClass?.hasInterfaceSuperClass() ?: false
}