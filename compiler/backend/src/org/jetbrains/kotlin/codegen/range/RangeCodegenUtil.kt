/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitiveNumberClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType

fun isPrimitiveRange(rangeType: KotlinType) =
    isClassTypeWithFqn(rangeType, PRIMITIVE_RANGE_FQNS)

fun isUnsignedRange(rangeType: KotlinType): Boolean =
    isClassTypeWithFqn(rangeType, UNSIGNED_RANGE_FQNS)

fun isPrimitiveProgression(rangeType: KotlinType) =
    isClassTypeWithFqn(rangeType, PRIMITIVE_PROGRESSION_FQNS)

fun isUnsignedProgression(rangeType: KotlinType) =
    isClassTypeWithFqn(rangeType, UNSIGNED_PROGRESSION_FQNS)

private val KotlinType.classFqnString: String?
    get() {
        val declarationDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return null
        val fqn = DescriptorUtils.getFqName(declarationDescriptor)
        return if (fqn.isSafe) fqn.asString() else null
    }

private fun isClassTypeWithFqn(kotlinType: KotlinType, fqns: Set<String>): Boolean =
    kotlinType.classFqnString in fqns

internal const val CHAR_RANGE_FQN = "kotlin.ranges.CharRange"
internal const val INT_RANGE_FQN = "kotlin.ranges.IntRange"
internal const val LONG_RANGE_FQN = "kotlin.ranges.LongRange"
private val PRIMITIVE_RANGE_FQNS = setOf(CHAR_RANGE_FQN, INT_RANGE_FQN, LONG_RANGE_FQN)

internal const val CHAR_PROGRESSION_FQN = "kotlin.ranges.CharProgression"
internal const val INT_PROGRESSION_FQN = "kotlin.ranges.IntProgression"
internal const val LONG_PROGRESSION_FQN = "kotlin.ranges.LongProgression"
private val PRIMITIVE_PROGRESSION_FQNS = setOf(CHAR_PROGRESSION_FQN, INT_PROGRESSION_FQN, LONG_PROGRESSION_FQN)

private const val CLOSED_FLOAT_RANGE_FQN = "kotlin.ranges.ClosedFloatRange"
private const val CLOSED_DOUBLE_RANGE_FQN = "kotlin.ranges.ClosedDoubleRange"
private const val CLOSED_RANGE_FQN = "kotlin.ranges.ClosedRange"
private const val CLOSED_FLOATING_POINT_RANGE_FQN = "kotlin.ranges.ClosedFloatingPointRange"
private const val COMPARABLE_RANGE_FQN = "kotlin.ranges.ComparableRange"

internal const val UINT_RANGE_FQN = "kotlin.ranges.UIntRange"
internal const val ULONG_RANGE_FQN = "kotlin.ranges.ULongRange"
private val UNSIGNED_RANGE_FQNS = setOf(UINT_RANGE_FQN, ULONG_RANGE_FQN)

internal const val UINT_PROGRESSION_FQN = "kotlin.ranges.UIntProgression"
internal const val ULONG_PROGRESSION_FQN = "kotlin.ranges.ULongProgression"
private val UNSIGNED_PROGRESSION_FQNS = setOf(UINT_PROGRESSION_FQN, ULONG_PROGRESSION_FQN)

fun getRangeOrProgressionElementType(rangeType: KotlinType): KotlinType? {
    val rangeClassDescriptor = rangeType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    val builtIns = rangeClassDescriptor.builtIns

    return when (rangeClassDescriptor.fqNameSafe.asString()) {
        CHAR_RANGE_FQN, CHAR_PROGRESSION_FQN -> builtIns.charType
        INT_RANGE_FQN, INT_PROGRESSION_FQN -> builtIns.intType
        LONG_RANGE_FQN, LONG_PROGRESSION_FQN -> builtIns.longType

        CLOSED_FLOAT_RANGE_FQN -> builtIns.floatType
        CLOSED_DOUBLE_RANGE_FQN -> builtIns.doubleType

        CLOSED_RANGE_FQN -> rangeType.arguments.singleOrNull()?.type
        CLOSED_FLOATING_POINT_RANGE_FQN -> rangeType.arguments.singleOrNull()?.type
        COMPARABLE_RANGE_FQN -> rangeType.arguments.singleOrNull()?.type

        UINT_RANGE_FQN, UINT_PROGRESSION_FQN ->
            rangeClassDescriptor.findTypeInModuleByTopLevelClassFqName(KotlinBuiltIns.FQ_NAMES.uIntFqName)

        ULONG_RANGE_FQN, ULONG_PROGRESSION_FQN ->
            rangeClassDescriptor.findTypeInModuleByTopLevelClassFqName(KotlinBuiltIns.FQ_NAMES.uLongFqName)

        else -> null
    }
}

private fun DeclarationDescriptor.findTypeInModuleByTopLevelClassFqName(fqName: FqName) =
    module.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))?.defaultType

fun BindingContext.getElementType(forExpression: KtForExpression): KotlinType {
    val loopRange = forExpression.loopRange!!
    val nextCall = get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, loopRange)
        ?: throw AssertionError("No next() function " + PsiDiagnosticUtils.atLocation(loopRange))
    return nextCall.resultingDescriptor.returnType!!
}

fun isPrimitiveNumberRangeTo(rangeTo: CallableDescriptor) =
    "rangeTo" == rangeTo.name.asString() && isPrimitiveNumberClassDescriptor(rangeTo.containingDeclaration) ||
            isPrimitiveRangeToExtension(rangeTo)

fun isUnsignedIntegerRangeTo(rangeTo: CallableDescriptor) =
    "rangeTo" == rangeTo.name.asString() && isUnsignedIntegerClassDescriptor(rangeTo.containingDeclaration)

fun isUnsignedIntegerClassDescriptor(descriptor: DeclarationDescriptor?) =
    descriptor != null && UnsignedTypes.isUnsignedClass(descriptor)

private inline fun CallableDescriptor.isTopLevelExtensionOnType(
    name: String,
    packageFQN: String,
    receiverTypePredicate: (KotlinType) -> Boolean
): Boolean {
    if (!this.isTopLevelInPackage(name, packageFQN)) return false
    val extensionReceiverType = original.extensionReceiverParameter?.type ?: return false
    return receiverTypePredicate(extensionReceiverType)
}

private fun isPrimitiveRangeToExtension(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("rangeTo", "kotlin.ranges") {
        KotlinBuiltIns.isPrimitiveType(it)
    }

fun isPrimitiveNumberDownTo(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("downTo", "kotlin.ranges") {
        isPrimitiveNumberClassDescriptor(it.constructor.declarationDescriptor)
    }

fun isUnsignedIntegerDownTo(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("downTo", "kotlin.ranges") {
        isUnsignedIntegerClassDescriptor(it.constructor.declarationDescriptor)
    }

fun isPrimitiveNumberUntil(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("until", "kotlin.ranges") {
        isPrimitiveNumberClassDescriptor(it.constructor.declarationDescriptor)
    }

fun isUnsignedIntegerUntil(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("until", "kotlin.ranges") {
        isUnsignedIntegerClassDescriptor(it.constructor.declarationDescriptor)
    }

fun isArrayOrPrimitiveArrayIndices(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("indices", "kotlin.collections") {
        KotlinBuiltIns.isArray(it) || KotlinBuiltIns.isPrimitiveArray(it)
    }

fun isArrayOrPrimitiveArrayWithIndex(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("withIndex", "kotlin.collections") {
        KotlinBuiltIns.isArray(it) || KotlinBuiltIns.isPrimitiveArray(it)
    }

fun isCollectionIndices(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("indices", "kotlin.collections") {
        KotlinBuiltIns.isCollectionOrNullableCollection(it)
    }

fun isIterableWithIndex(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("withIndex", "kotlin.collections") {
        KotlinBuiltIns.isIterableOrNullableIterable(it)
    }

fun isSequenceWithIndex(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("withIndex", "kotlin.sequences") {
        val typeDescriptor = it.constructor.declarationDescriptor ?: return false
        typeDescriptor.isTopLevelInPackage("Sequence", "kotlin.sequences")
    }

fun isCharSequenceIndices(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("indices", "kotlin.text") {
        KotlinBuiltIns.isCharSequenceOrNullableCharSequence(it)
    }

fun isCharSequenceWithIndex(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("withIndex", "kotlin.text") {
        KotlinBuiltIns.isCharSequenceOrNullableCharSequence(it)
    }

fun isComparableRangeTo(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("rangeTo", "kotlin.ranges") {
        val extensionReceiverTypeDescriptor = it.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
        val upperBoundType = extensionReceiverTypeDescriptor.upperBounds.singleOrNull() ?: return false
        val upperBoundClassDescriptor = upperBoundType.constructor.declarationDescriptor as? ClassDescriptor ?: return false
        upperBoundClassDescriptor.isTopLevelInPackage("Comparable", "kotlin")
    }

fun isClosedRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false
    val containingClassDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return false
    if (!containingClassDescriptor.isTopLevelInPackage("ClosedRange", "kotlin.ranges")) return false

    return true
}

fun isPrimitiveRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false
    val dispatchReceiverType = descriptor.dispatchReceiverParameter?.type ?: return false
    if (!isPrimitiveRange(dispatchReceiverType)) return false

    return true
}

fun isUnsignedIntegerRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false

    val dispatchReceiverType = descriptor.dispatchReceiverParameter?.type
    val extensionReceiverType = descriptor.extensionReceiverParameter?.type

    return (dispatchReceiverType != null && isUnsignedRange(dispatchReceiverType)) ||
            (extensionReceiverType != null && isUnsignedRange(extensionReceiverType))
}

fun isPrimitiveNumberRangeExtensionContainsPrimitiveNumber(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false

    val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return false

    val rangeElementType = getRangeOrProgressionElementType(extensionReceiverType) ?: return false
    if (!isPrimitiveNumberType(rangeElementType)) return false

    val argumentType = descriptor.valueParameters.singleOrNull()?.type ?: return false
    if (!isPrimitiveNumberType(argumentType)) return false

    return true
}

fun isPrimitiveProgressionReverse(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("reversed", "kotlin.ranges") {
        isPrimitiveProgression(it)
    }

private fun isPrimitiveNumberType(type: KotlinType) =
    KotlinBuiltIns.isByte(type) ||
            KotlinBuiltIns.isShort(type) ||
            KotlinBuiltIns.isInt(type) ||
            KotlinBuiltIns.isChar(type) ||
            KotlinBuiltIns.isLong(type) ||
            KotlinBuiltIns.isFloat(type) ||
            KotlinBuiltIns.isDouble(type)

fun isClosedFloatingPointRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false
    val containingClassDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return false
    if (!containingClassDescriptor.isTopLevelInPackage("ClosedFloatingPointRange", "kotlin.ranges")) return false

    return true
}

fun isCharSequenceIterator(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("iterator", "kotlin.text") {
        it.constructor.declarationDescriptor?.isTopLevelInPackage("CharSequence", "kotlin")
            ?: false
    }
