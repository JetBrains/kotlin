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
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
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

fun isRange(rangeType: KotlinType) =
        !rangeType.isMarkedNullable && getPrimitiveRangeElementType(rangeType) != null

fun isProgression(rangeType: KotlinType) =
        !rangeType.isMarkedNullable && getPrimitiveProgressionElementType(rangeType) != null

private fun getPrimitiveRangeElementType(rangeType: KotlinType) =
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
        "rangeTo" == rangeTo.name.asString() &&
        isPrimitiveNumberClassDescriptor(rangeTo.containingDeclaration)

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

fun isPrimitiveRangeToExtension(operationReference: KtSimpleNameExpression, bindingContext: BindingContext): Boolean {
    val resolvedCall = operationReference.getResolvedCallWithAssert(bindingContext)
    val receiver = resolvedCall.dispatchReceiver as? ExpressionReceiver ?: return false

    /*
     * Range is optimizable if
     * receiver is a call for 'rangeTo' from stdlib package
     * and its argument has same primitive type as generic range parameter.
     * For non-matching primitive types (e.g. int in double range)
     * dispatch receiver will be null, because extension method will be called.
     */

    val resolvedReceiver = receiver.expression.getResolvedCall(bindingContext) ?: return false
    return isPrimitiveRangeToExtension(resolvedReceiver.resultingDescriptor)
}

/*
 * Checks whether for expression 'x in a..b' a..b is primitive integral range
 * with same type as x.
 */
fun isPrimitiveRangeSpecializationOfType(
        argumentType: Type,
        rangeExpression: KtExpression,
        bindingContext: BindingContext
): Boolean {
    if (rangeExpression is KtBinaryExpression && rangeExpression.operationReference.getReferencedNameElementType() === KtTokens.RANGE) {
        val kotlinType = bindingContext.getType(rangeExpression)!!
        val descriptor = kotlinType.constructor.declarationDescriptor ?: return false
        val fqName = DescriptorUtils.getFqName(descriptor)
        return (fqName == KotlinBuiltIns.FQ_NAMES.longRange && argumentType === Type.LONG_TYPE) ||
               (fqName == KotlinBuiltIns.FQ_NAMES.charRange || fqName == KotlinBuiltIns.FQ_NAMES.intRange) && AsmUtil.isIntPrimitive(argumentType)
    }

    return false
}

private fun isTopLevelInPackage(descriptor: CallableDescriptor, name: String, packageName: String): Boolean {
    if (name != descriptor.name.asString()) return false

    val containingDeclaration = descriptor.containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}
