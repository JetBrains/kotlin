/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.RANGES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitiveNumberClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.PsiDiagnosticUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

val supportedRangeTypes = listOf(PrimitiveType.CHAR, PrimitiveType.INT, PrimitiveType.LONG)

private val RANGE_TO_ELEMENT_TYPE: Map<FqName, PrimitiveType> =
    supportedRangeTypes.associateBy {
        RANGES_PACKAGE_FQ_NAME.child(Name.identifier(it.typeName.toString() + "Range"))
    }

private val PROGRESSION_TO_ELEMENT_TYPE: Map<FqName, PrimitiveType> =
    supportedRangeTypes.associateBy {
        RANGES_PACKAGE_FQ_NAME.child(Name.identifier(it.typeName.toString() + "Progression"))
    }

fun isPrimitiveRange(rangeType: KotlinType) =
    !rangeType.isMarkedNullable && getPrimitiveRangeElementType(rangeType) != null

fun isPrimitiveProgression(rangeType: KotlinType) =
    !rangeType.isMarkedNullable && getPrimitiveProgressionElementType(rangeType) != null

fun getPrimitiveRangeElementType(rangeType: KotlinType): PrimitiveType? =
    getPrimitiveRangeOrProgressionElementType(rangeType, RANGE_TO_ELEMENT_TYPE)

private fun getPrimitiveProgressionElementType(rangeType: KotlinType) =
    getPrimitiveRangeOrProgressionElementType(rangeType, PROGRESSION_TO_ELEMENT_TYPE)

private fun getPrimitiveRangeOrProgressionElementType(
    rangeOrProgression: KotlinType,
    map: Map<FqName, PrimitiveType>
): PrimitiveType? {
    val declarationDescriptor = rangeOrProgression.constructor.declarationDescriptor ?: return null
    val fqName = DescriptorUtils.getFqName(declarationDescriptor).takeIf { it.isSafe } ?: return null
    return map[fqName.toSafe()]
}

fun getRangeOrProgressionElementType(rangeType: KotlinType): KotlinType? {
    val rangeTypeDescriptor = rangeType.constructor.declarationDescriptor ?: return null
    val builtIns = rangeTypeDescriptor.builtIns

    return when {
        rangeTypeDescriptor.isTopLevelInPackage("CharRange", "kotlin.ranges") -> builtIns.charType
        rangeTypeDescriptor.isTopLevelInPackage("IntRange", "kotlin.ranges") -> builtIns.intType
        rangeTypeDescriptor.isTopLevelInPackage("LongRange", "kotlin.ranges") -> builtIns.longType

        rangeTypeDescriptor.isTopLevelInPackage("CharProgression", "kotlin.ranges") -> builtIns.charType
        rangeTypeDescriptor.isTopLevelInPackage("IntProgression", "kotlin.ranges") -> builtIns.intType
        rangeTypeDescriptor.isTopLevelInPackage("LongProgression", "kotlin.ranges") -> builtIns.longType

        rangeTypeDescriptor.isTopLevelInPackage("ClosedFloatRange", "kotlin.ranges") -> builtIns.floatType
        rangeTypeDescriptor.isTopLevelInPackage("ClosedDoubleRange", "kotlin.ranges") -> builtIns.doubleType

        rangeTypeDescriptor.isTopLevelInPackage("ClosedRange", "kotlin.ranges") -> rangeType.arguments.singleOrNull()?.type

        rangeTypeDescriptor.isTopLevelInPackage("ClosedFloatingPointRange", "kotlin.ranges") -> rangeType.arguments.singleOrNull()?.type

        rangeTypeDescriptor.isTopLevelInPackage("ComparableRange", "kotlin.ranges") -> rangeType.arguments.singleOrNull()?.type

        else -> null
    }
}

fun BindingContext.getElementType(forExpression: KtForExpression): KotlinType {
    val loopRange = forExpression.loopRange!!
    val nextCall = get(BindingContext.LOOP_RANGE_NEXT_RESOLVED_CALL, loopRange)
            ?: throw AssertionError("No next() function " + PsiDiagnosticUtils.atLocation(loopRange))
    return nextCall.resultingDescriptor.returnType!!
}

fun getPrimitiveRangeOrProgressionElementType(rangeOrProgressionName: FqName): PrimitiveType? =
    RANGE_TO_ELEMENT_TYPE[rangeOrProgressionName] ?: PROGRESSION_TO_ELEMENT_TYPE[rangeOrProgressionName]

fun isRangeOrProgression(className: FqName) =
    getPrimitiveRangeOrProgressionElementType(className) != null

fun isPrimitiveNumberRangeTo(rangeTo: CallableDescriptor) =
    "rangeTo" == rangeTo.name.asString() && isPrimitiveNumberClassDescriptor(rangeTo.containingDeclaration) ||
            isPrimitiveRangeToExtension(rangeTo)

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

fun isPrimitiveNumberUntil(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("until", "kotlin.ranges") {
        isPrimitiveNumberClassDescriptor(it.constructor.declarationDescriptor)
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

fun getClosedFloatingPointRangeElementType(rangeType: KotlinType): KotlinType? {
    val classDescriptor = rangeType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    if (!classDescriptor.isTopLevelInPackage("ClosedFloatingPointRange", "kotlin.ranges")) return null
    return rangeType.arguments.singleOrNull()?.type
}

fun DeclarationDescriptor.isTopLevelInPackage(name: String, packageName: String): Boolean {
    if (name != this.name.asString()) return false

    val containingDeclaration = containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}

fun getAsmRangeElementTypeForPrimitiveRangeOrProgression(rangeCallee: CallableDescriptor): Type {
    val rangeType = rangeCallee.returnType!!

    getPrimitiveRangeElementType(rangeType)?.let {
        return AsmTypes.valueTypeForPrimitive(it)
    }

    getPrimitiveProgressionElementType(rangeType)?.let {
        return AsmTypes.valueTypeForPrimitive(it)
    }

    getClosedFloatingPointRangeElementType(rangeType)?.let {
        when {
            KotlinBuiltIns.isDouble(it) -> return Type.DOUBLE_TYPE
            KotlinBuiltIns.isFloat(it) -> return Type.FLOAT_TYPE
            else -> {
            }
        }
    }

    throw AssertionError("Unexpected range type: $rangeType")
}

fun isCharSequenceIterator(descriptor: CallableDescriptor) =
    descriptor.isTopLevelExtensionOnType("iterator", "kotlin.text") {
        it.constructor.declarationDescriptor?.isTopLevelInPackage("CharSequence", "kotlin")
                ?: false
    }
