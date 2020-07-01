/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.descriptors

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.longName
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.utils.addToStdlib.cast
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

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

internal val IrFunction.isTypedIntrinsic: Boolean
    get() = annotations.hasAnnotation(KonanFqNames.typedIntrinsic)

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

internal val arraysWithFixedSizeItems = setOf(
        "kotlin.ByteArray",
        "kotlin.CharArray",
        "kotlin.ShortArray",
        "kotlin.IntArray",
        "kotlin.LongArray",
        "kotlin.FloatArray",
        "kotlin.DoubleArray",
        "kotlin.BooleanArray"
)

internal val IrClass.isArray: Boolean
    get() = this.fqNameForIrSerialization.asString() in arrayTypes

internal val IrClass.isArrayWithFixedSizeItems: Boolean
    get() = this.fqNameForIrSerialization.asString() in arraysWithFixedSizeItems

fun IrClass.isAbstract() = this.modality == Modality.SEALED || this.modality == Modality.ABSTRACT

internal fun IrFunction.hasValueTypeAt(index: Int): Boolean {
    when (index) {
        0 -> return !isSuspend && returnType.let { (it.isInlinedNative() || it.isUnit()) }
        1 -> return dispatchReceiverParameter.let { it != null && it.type.isInlinedNative() }
        2 -> return extensionReceiverParameter.let { it != null && it.type.isInlinedNative() }
        else -> return this.valueParameters[index - 3].type.isInlinedNative()
    }
}

internal fun IrFunction.hasReferenceAt(index: Int): Boolean {
    when (index) {
        0 -> return isSuspend || returnType.let { !it.isInlinedNative() && !it.isUnit() }
        1 -> return dispatchReceiverParameter.let { it != null && !it.type.isInlinedNative() }
        2 -> return extensionReceiverParameter.let { it != null && !it.type.isInlinedNative() }
        else -> return !this.valueParameters[index - 3].type.isInlinedNative()
    }
}

private fun IrFunction.needBridgeToAt(target: IrFunction, index: Int)
        = hasValueTypeAt(index) xor target.hasValueTypeAt(index)

internal fun IrFunction.needBridgeTo(target: IrFunction)
        = (0..this.valueParameters.size + 2).any { needBridgeToAt(target, it) }

internal enum class BridgeDirection {
    NOT_NEEDED,
    FROM_VALUE_TYPE,
    TO_VALUE_TYPE
}

private fun IrFunction.bridgeDirectionToAt(target: IrFunction, index: Int)
       = when {
            hasValueTypeAt(index) && target.hasReferenceAt(index) -> BridgeDirection.FROM_VALUE_TYPE
            hasReferenceAt(index) && target.hasValueTypeAt(index) -> BridgeDirection.TO_VALUE_TYPE
            else -> BridgeDirection.NOT_NEEDED
        }

internal class BridgeDirections(val array: Array<BridgeDirection>) {
    constructor(parametersCount: Int): this(Array<BridgeDirection>(parametersCount + 3, { BridgeDirection.NOT_NEEDED }))

    fun allNotNeeded(): Boolean = array.all { it == BridgeDirection.NOT_NEEDED }

    override fun toString(): String {
        val result = StringBuilder()
        array.forEach {
            result.append(when (it) {
                BridgeDirection.FROM_VALUE_TYPE -> 'U' // unbox
                BridgeDirection.TO_VALUE_TYPE   -> 'B' // box
                BridgeDirection.NOT_NEEDED      -> 'N' // none
            })
        }
        return result.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BridgeDirections) return false

        return array.size == other.array.size
                && array.indices.all { array[it] == other.array[it] }
    }

    override fun hashCode(): Int {
        var result = 0
        array.forEach { result = result * 31 + it.ordinal }
        return result
    }
}

val IrSimpleFunction.allOverriddenFunctions: Set<IrSimpleFunction>
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

internal fun IrSimpleFunction.bridgeDirectionsTo(
        overriddenDescriptor: IrSimpleFunction
): BridgeDirections {
    val ourDirections = BridgeDirections(this.valueParameters.size)
    for (index in ourDirections.array.indices)
        ourDirections.array[index] = this.bridgeDirectionToAt(overriddenDescriptor, index)

    val target = this.target
    if (!this.isReal && modality != Modality.ABSTRACT
            && target.overrides(overriddenDescriptor)
            && ourDirections == target.bridgeDirectionsTo(overriddenDescriptor)) {
        // Bridge is inherited from superclass.
        return BridgeDirections(this.valueParameters.size)
    }

    return ourDirections
}

internal tailrec fun IrDeclaration.findPackage(): IrPackageFragment {
    val parent = this.parent
    return parent as? IrPackageFragment
            ?: (parent as IrDeclaration).findPackage()
}

fun IrFunctionSymbol.isComparisonFunction(map: Map<IrClassifierSymbol, IrSimpleFunctionSymbol>): Boolean =
        this in map.values

val IrDeclaration.isPropertyAccessor get() =
    this is IrSimpleFunction && this.correspondingPropertySymbol != null

val IrDeclaration.isPropertyField get() =
    this is IrField && this.correspondingPropertySymbol != null

val IrDeclaration.isTopLevelDeclaration get() =
    parent !is IrDeclaration && !this.isPropertyAccessor && !this.isPropertyField

fun IrDeclaration.findTopLevelDeclaration(): IrDeclaration = when {
    this.isTopLevelDeclaration ->
        this
    this.isPropertyAccessor ->
        (this as IrSimpleFunction).correspondingPropertySymbol!!.owner.findTopLevelDeclaration()
    this.isPropertyField ->
        (this as IrField).correspondingPropertySymbol!!.owner.findTopLevelDeclaration()
    else ->
        (this.parent as IrDeclaration).findTopLevelDeclaration()
}

internal val IrClass.isFrozen: Boolean
    get() = annotations.hasAnnotation(KonanFqNames.frozen) ||
            // RTTI is used for non-reference type box:
            !this.defaultType.binaryTypeIsReference()

fun IrConstructorCall.getAnnotationStringValue() = getValueArgument(0).safeAs<IrConst<String>>()?.value

fun IrConstructorCall.getAnnotationStringValue(name: String): String {
    val parameter = symbol.owner.valueParameters.single { it.name.asString() == name }
    return getValueArgument(parameter.index).cast<IrConst<String>>().value
}

fun AnnotationDescriptor.getAnnotationStringValue(name: String): String {
    return argumentValue(name)?.safeAs<StringValue>()?.value ?: error("Expected value $name at annotation $this")
}

fun <T> IrConstructorCall.getAnnotationValueOrNull(name: String): T? {
    val parameter = symbol.owner.valueParameters.atMostOne { it.name.asString() == name }
    return parameter?.let { getValueArgument(it.index)?.let { (it.cast<IrConst<T>>()).value } }
}

fun IrFunction.externalSymbolOrThrow(): String? {
    annotations.findAnnotation(RuntimeNames.symbolNameAnnotation)?.let { return it.getAnnotationStringValue() }

    if (annotations.hasAnnotation(KonanFqNames.objCMethod)) return null

    if (annotations.hasAnnotation(KonanFqNames.typedIntrinsic)) return null

    if (annotations.hasAnnotation(RuntimeNames.cCall)) return null

    throw Error("external function ${this.longName} must have @TypedIntrinsic, @SymbolName or @ObjCMethod annotation")
}

val IrFunction.isBuiltInOperator get() = origin == IrBuiltIns.BUILTIN_OPERATOR

fun IrDeclaration.isFromMetadataInteropLibrary() =
        descriptor.module.isFromInteropLibrary()