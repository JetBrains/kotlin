/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.interfaceId
import org.jetbrains.kotlin.ir.backend.js.interfaceUsedAsTypeOperand
import org.jetbrains.kotlin.ir.backend.js.interfaceUsedInReflection
import org.jetbrains.kotlin.ir.backend.js.utils.JsStaticContext
import org.jetbrains.kotlin.ir.backend.js.utils.getClassRef
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.irAttribute
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isSuspendFunctionOrKFunction
import org.jetbrains.kotlin.js.backend.ast.JsArrayLiteral
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
import org.jetbrains.kotlin.utils.toSmartList
import kotlin.math.max

private var IrClass.implementedInterfacesMask: IntArray? by irAttribute(copyByDefault = false)

// The logic in this function must be kept in sync with `kotlin.js.bitMaskWith` in stdlib
private fun bitMaskWith(activeBit: Int): IntArray {
    val numberIndex = activeBit / 32
    val intArray = IntArray(numberIndex + 1)
    val positionInNumber = activeBit and 31
    val numberWithSettledBit = 1 shl positionInNumber
    intArray[numberIndex] = intArray[numberIndex] or numberWithSettledBit
    return intArray
}

// The logic in this function must be kept in sync with `kotlin.js.compositeBitMask` in stdlib
private fun compositeBitMask(capacity: Int, masks: List<IntArray>): IntArray {
    return IntArray(capacity) { i ->
        var result = 0
        for (mask in masks) {
            if (i < mask.size) {
                result = result or mask[i]
            }
        }
        result
    }
}

private fun IrClass.implementedInterfacesMask(): IntArray {
    implementedInterfacesMask?.let { return it }
    return interfacesBitMask(
        superTypes
            .filter { it.classOrNull?.owner?.isExternal != true }
            .mapNotNull { it.asSuperclassForMetadata() }
    ).also {
        implementedInterfacesMask = it
    }
}

// The logic in this function must be kept in sync with `kotlin.js.dynamicallyCreateInterfaceMask` in stdlib
internal fun interfacesBitMask(superClasses: List<IrClass>): IntArray {
    var maxSize = 0
    val masks = mutableListOf<IntArray>()
    for (superClass in superClasses) {
        var currentSize = maxSize
        val imask = superClass.implementedInterfacesMask()
        if (imask.isNotEmpty()) {
            masks.add(imask)
            currentSize = imask.size
        }

        val iidMask = superClass.interfaceId
            ?.takeIf { superClass.interfaceUsedAsTypeOperand || superClass.interfaceUsedInReflection }
            ?.let { bitMaskWith(it) }

        if (iidMask != null) {
            masks.add(iidMask)
            currentSize = max(currentSize, iidMask.size)
        }

        if (currentSize > maxSize) {
            maxSize = currentSize
        }
    }

    return compositeBitMask(maxSize, masks)
}

internal fun IrClass.superInterfacesForMetadata(): List<IrClass>? {
    val baseClass = superTypes.firstOrNull { !it.classOrFail.owner.isInterface }
    return superTypes
        .filter { it.classOrNull?.owner?.isExternal != true }
        .takeIf { it.size > 1 || it.singleOrNull() != baseClass }
        ?.mapNotNull { it.asSuperclassForMetadata() }
        ?.takeIf { it.isNotEmpty() }
}

private fun IrType.asSuperclassForMetadata(): IrClass? {
    val ownerSymbol = classOrNull?.takeIf {
        !isAny() && !isFunctionType() && !it.owner.isEffectivelyExternal()
    } ?: return null
    return ownerSymbol.owner
}

private fun IrType.isFunctionType() = isFunctionOrKFunction() || isSuspendFunctionOrKFunction()
