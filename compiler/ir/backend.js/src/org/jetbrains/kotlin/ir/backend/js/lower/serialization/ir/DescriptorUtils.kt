/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassNotAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.isUnit

/**
 * List of all implemented interfaces (including those which implemented by a super class)
 */
internal val IrClass.implementedInterfaces: List<IrClass>
    get() {
        val superClassImplementedInterfaces = this.getSuperClassNotAny()?.implementedInterfaces ?: emptyList()
        val superInterfaces = this.getSuperInterfaces()
        val superInterfacesImplementedInterfaces = superInterfaces.flatMap { it.implementedInterfaces }
        return (superClassImplementedInterfaces +
                superInterfacesImplementedInterfaces +
                superInterfaces).distinct()
    }


/**
 * Implementation of given method.
 *
 * TODO: this method is actually a part of resolve and probably duplicates another one
 */
internal fun IrSimpleFunction.resolveFakeOverride(allowAbstract: Boolean = false): IrSimpleFunction {
    if (this.isReal) {
        return this
    }

    val visited = mutableSetOf<IrSimpleFunction>()
    val realSupers = mutableSetOf<IrSimpleFunction>()

    fun findRealSupers(function: IrSimpleFunction) {
        if (function in visited) return
        visited += function
        if (function.isReal) {
            realSupers += function
        } else {
            function.overriddenSymbols.forEach { findRealSupers(it.owner) }
        }
    }

    findRealSupers(this)

    if (realSupers.size > 1) {
        visited.clear()

        fun excludeOverridden(function: IrSimpleFunction) {
            if (function in visited) return
            visited += function
            function.overriddenSymbols.forEach {
                realSupers.remove(it.owner)
                excludeOverridden(it.owner)
            }
        }

        realSupers.toList().forEach { excludeOverridden(it) }
    }

    return realSupers.first { allowAbstract || it.modality != Modality.ABSTRACT }
}

// TODO: don't forget to remove descriptor access here.
//internal val FunctionDescriptor.isTypedIntrinsic: Boolean
//    get() = this.descriptor.isTypedIntrinsic
//
//internal val DeclarationDescriptor.isFrozen: Boolean
//    get() = this.descriptor.isFrozen

internal val arrayTypes = setOf(
    "kotlin.Array",
    "kotlin.ByteArray",
    "kotlin.CharArray",
    "kotlin.ShortArray",
    "kotlin.IntArray",
    "kotlin.LongArray",
    "kotlin.FloatArray",
    "kotlin.DoubleArray",
    "kotlin.BooleanArray",
    "kotlin.native.ImmutableBlob",
    "kotlin.native.internal.NativePtrArray"
)


internal val IrClass.isArray: Boolean
    get() = this.fqNameSafe.asString() in arrayTypes


internal val IrClass.isInterface: Boolean
    get() = (this.kind == ClassKind.INTERFACE)

fun IrClass.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT
        || this.kind == ClassKind.ENUM_CLASS

//internal fun FunctionDescriptor.hasValueTypeAt(index: Int): Boolean {
//    when (index) {
//        0 -> return !isSuspend && returnType.let { (it.isInlined() || it.isUnit()) }
//        1 -> return dispatchReceiverParameter.let { it != null && it.type.isInlined() }
//        2 -> return extensionReceiverParameter.let { it != null && it.type.isInlined() }
//        else -> return this.valueParameters[index - 3].type.isInlined()
//    }
//}
//
//internal fun FunctionDescriptor.hasReferenceAt(index: Int): Boolean {
//    when (index) {
//        0 -> return isSuspend || returnType.let { !it.isInlined() && !it.isUnit() }
//        1 -> return dispatchReceiverParameter.let { it != null && !it.type.isInlined() }
//        2 -> return extensionReceiverParameter.let { it != null && !it.type.isInlined() }
//        else -> return !this.valueParameters[index - 3].type.isInlined()
//    }
//}

//private fun FunctionDescriptor.needBridgeToAt(target: FunctionDescriptor, index: Int)
//        = hasValueTypeAt(index) xor target.hasValueTypeAt(index)

//internal fun FunctionDescriptor.needBridgeTo(target: FunctionDescriptor)
//        = (0..this.valueParameters.size + 2).any { needBridgeToAt(target, it) }

internal val IrSimpleFunction.target: IrSimpleFunction
    get() = (if (modality == Modality.ABSTRACT) this else resolveFakeOverride()).original

internal val IrFunction.target: IrFunction get() = when (this) {
    is IrSimpleFunction -> this.target
    is IrConstructor -> this
    else -> error(this)
}

//private fun FunctionDescriptor.bridgeDirectionToAt(target: FunctionDescriptor, index: Int)
//        = when {
//    hasValueTypeAt(index) && target.hasReferenceAt(index) -> BridgeDirection.FROM_VALUE_TYPE
//    hasReferenceAt(index) && target.hasValueTypeAt(index) -> BridgeDirection.TO_VALUE_TYPE
//    else -> BridgeDirection.NOT_NEEDED
//}

val IrSimpleFunction.allOverriddenDescriptors: Set<IrSimpleFunction>
    get() {
        val result = mutableSetOf<IrSimpleFunction>()

        fun traverse(function: IrSimpleFunction) {
            if (function in result) return
            result += function
            function.overriddenSymbols.forEach { traverse(it.owner) }
        }

        traverse(this)

        return result
    }

//internal fun IrSimpleFunction.bridgeDirectionsTo(
//    overriddenDescriptor: IrSimpleFunction
//): BridgeDirections {
//    val ourDirections = BridgeDirections(this.valueParameters.size)
//    for (index in ourDirections.array.indices)
//        ourDirections.array[index] = this.bridgeDirectionToAt(overriddenDescriptor, index)
//
//    val target = this.target
//    if (!this.isReal && modality != Modality.ABSTRACT
//        && target.overrides(overriddenDescriptor)
//        && ourDirections == target.bridgeDirectionsTo(overriddenDescriptor)) {
//        // Bridge is inherited from superclass.
//        return BridgeDirections(this.valueParameters.size)
//    }
//
//    return ourDirections
//}

tailrec internal fun IrDeclaration.findPackage(): IrPackageFragment {
    val parent = this.parent
    return parent as? IrPackageFragment
        ?: (parent as IrDeclaration).findPackage()
}

fun IrFunction.isComparisonDescriptor(map: Map<SimpleType, IrSimpleFunction>): Boolean =
    this in map.values

val IrDeclaration.isPropertyAccessor get() =
    this is IrSimpleFunction && this.correspondingProperty != null

val IrDeclaration.isPropertyField get() =
    this is IrField && this.correspondingProperty != null

val IrDeclaration.isTopLevelDeclaration get() =
    parent !is IrDeclaration && !this.isPropertyAccessor && !this.isPropertyField

fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when {
    this.isTopLevelDeclaration ->
        this
    this.isPropertyAccessor ->
        (this as IrSimpleFunction).correspondingProperty!!.findTopLevelDeclaration()
    this.isPropertyField ->
        (this as IrField).correspondingProperty!!.findTopLevelDeclaration()
    else ->
        (this.parent as IrDeclaration).findTopLevelDeclaration()
}