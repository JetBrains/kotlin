/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.RANGES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitiveNumberClassDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Type

private val RANGE_TO_ELEMENT_TYPE: Map<FqName, PrimitiveType> =
        supportedRangeTypes().associateBy {
            RANGES_PACKAGE_FQ_NAME.child(Name.identifier(it.typeName.toString() + "Range"))
        }

private val PROGRESSION_TO_ELEMENT_TYPE: Map<FqName, PrimitiveType> =
        supportedRangeTypes().associateBy {
            RANGES_PACKAGE_FQ_NAME.child(Name.identifier(it.typeName.toString() + "Progression"))
        }

fun supportedRangeTypes() =
        listOf(PrimitiveType.CHAR, PrimitiveType.INT, PrimitiveType.LONG)

fun isPrimitiveRange(rangeType: KotlinType) =
        !rangeType.isMarkedNullable && getPrimitiveRangeElementType(rangeType) != null

fun isPrimitiveProgression(rangeType: KotlinType) =
        !rangeType.isMarkedNullable && getPrimitiveProgressionElementType(rangeType) != null

fun getPrimitiveRangeElementType(rangeType: KotlinType) =
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

fun getPrimitiveRangeOrProgressionElementType(rangeOrProgressionName: FqName): PrimitiveType? =
        RANGE_TO_ELEMENT_TYPE[rangeOrProgressionName] ?:
        PROGRESSION_TO_ELEMENT_TYPE[rangeOrProgressionName]

fun isRangeOrProgression(className: FqName) =
        getPrimitiveRangeOrProgressionElementType(className) != null

fun isPrimitiveNumberRangeTo(rangeTo: CallableDescriptor) =
        "rangeTo" == rangeTo.name.asString() && isPrimitiveNumberClassDescriptor(rangeTo.containingDeclaration) ||
        isPrimitiveRangeToExtension(rangeTo)

private fun isPrimitiveRangeToExtension(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "rangeTo", "kotlin.ranges")) return false

    val extensionReceiver = descriptor.extensionReceiverParameter ?: return false
    return KotlinBuiltIns.isPrimitiveType(extensionReceiver.type)
}

fun isPrimitiveNumberDownTo(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "downTo", "kotlin.ranges")) return false

    val extensionReceiver = descriptor.extensionReceiverParameter ?: return false
    val extensionReceiverClassifier = extensionReceiver.type.constructor.declarationDescriptor
    return isPrimitiveNumberClassDescriptor(extensionReceiverClassifier)
}

fun isPrimitiveNumberUntil(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "until", "kotlin.ranges")) return false

    val extensionReceiver = descriptor.extensionReceiverParameter ?: return false
    val extensionReceiverClassifier = extensionReceiver.type.constructor.declarationDescriptor
    return isPrimitiveNumberClassDescriptor(extensionReceiverClassifier)
}

fun isArrayOrPrimitiveArrayIndices(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "indices", "kotlin.collections")) return false

    val extensionReceiver = descriptor.extensionReceiverParameter ?: return false
    val extensionReceiverType = extensionReceiver.type
    return KotlinBuiltIns.isArray(extensionReceiverType) || KotlinBuiltIns.isPrimitiveArray(extensionReceiverType)
}

fun isCollectionIndices(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "indices", "kotlin.collections")) return false

    val extensionReceiver = descriptor.extensionReceiverParameter ?: return false
    val extensionReceiverType = extensionReceiver.type
    return KotlinBuiltIns.isCollectionOrNullableCollection(extensionReceiverType)
}

fun isCharSequenceIndices(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "indices", "kotlin.text")) return false

    val extensionReceiver = descriptor.extensionReceiverParameter ?: return false
    val extensionReceiverType = extensionReceiver.type
    return KotlinBuiltIns.isCharSequenceOrNullableCharSequence(extensionReceiverType)
}

fun isComparableRangeTo(descriptor: CallableDescriptor): Boolean {
    if (!isTopLevelInPackage(descriptor, "rangeTo", "kotlin.ranges")) return false

    val extensionReceiver = descriptor.original.extensionReceiverParameter ?: return false
    val extensionReceiverTypeDescriptor = extensionReceiver.type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    val upperBoundType = extensionReceiverTypeDescriptor.upperBounds.singleOrNull() ?: return false
    val upperBoundClassDescriptor = upperBoundType.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    if (!isTopLevelInPackage(upperBoundClassDescriptor, "Comparable", "kotlin")) return false

    return true
}

fun isClosedRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false
    val containingClassDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return false
    if (!isTopLevelInPackage(containingClassDescriptor, "ClosedRange", "kotlin.ranges")) return false

    return true
}

fun isPrimitiveRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false
    val dispatchReceiverType = descriptor.dispatchReceiverParameter?.type ?: return false
    if (!isPrimitiveRange(dispatchReceiverType)) return false

    return true
}

fun isIntPrimitiveRangeExtensionForInt(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false

    val extensionReceiverType = descriptor.extensionReceiverParameter?.type ?: return false
    val extensionReceiverClassDescriptor = extensionReceiverType.constructor.declarationDescriptor as? ClassDescriptor ?: return false
    if (!isTopLevelInPackage(extensionReceiverClassDescriptor, "ClosedRange", "kotlin.ranges")) return false

    val rangeElementType = extensionReceiverType.arguments.singleOrNull()?.type ?: return false
    if (!isIntPrimitiveType(rangeElementType)) return false

    val argumentType = descriptor.valueParameters.singleOrNull()?.type ?: return false
    if (!isIntPrimitiveType(argumentType)) return false

    return true
}

private fun isIntPrimitiveType(type: KotlinType) =
        KotlinBuiltIns.isByte(type) ||
        KotlinBuiltIns.isShort(type) ||
        KotlinBuiltIns.isInt(type)

fun isClosedFloatingPointRangeContains(descriptor: CallableDescriptor): Boolean {
    if (descriptor.name.asString() != "contains") return false
    val containingClassDescriptor = descriptor.containingDeclaration as? ClassDescriptor ?: return false
    if (!isTopLevelInPackage(containingClassDescriptor, "ClosedFloatingPointRange", "kotlin.ranges")) return false

    return true
}

fun getClosedFloatingPointRangeElementType(rangeType: KotlinType): KotlinType? {
    val classDescriptor = rangeType.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    if (!isTopLevelInPackage(classDescriptor, "ClosedFloatingPointRange", "kotlin.ranges")) return null
    return rangeType.arguments.singleOrNull()?.type
}

private fun isTopLevelInPackage(descriptor: DeclarationDescriptor, name: String, packageName: String): Boolean {
    if (name != descriptor.name.asString()) return false

    val containingDeclaration = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}

fun getAsmRangeElementTypeForPrimitiveRange(rangeCallee: CallableDescriptor): Type {
    val rangeType = rangeCallee.returnType!!

    getPrimitiveRangeElementType(rangeType)?.let {
        return AsmTypes.valueTypeForPrimitive(it)
    }

    val floatingPointElementType = getClosedFloatingPointRangeElementType(rangeType) ?:
                                   throw AssertionError("Unexpected range type: $rangeType")

    return when {
        KotlinBuiltIns.isDouble(floatingPointElementType) -> Type.DOUBLE_TYPE
        KotlinBuiltIns.isFloat(floatingPointElementType) -> Type.FLOAT_TYPE
        else -> throw AssertionError("Unexpected ClosedFloatingPointRange element type: $floatingPointElementType")
    }
}
