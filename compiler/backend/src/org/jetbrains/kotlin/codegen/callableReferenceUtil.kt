/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

fun capturedBoundReferenceReceiver(ownerType: Type, expectedReceiverType: Type, isInliningStrategy: Boolean): StackValue =
        StackValue.operation(expectedReceiverType) { iv ->
            iv.load(0, ownerType)
            iv.getfield(
                ownerType.internalName,
                    //HACK for inliner - it should recognize field as captured receiver
                if (isInliningStrategy) AsmUtil.CAPTURED_RECEIVER_FIELD else AsmUtil.BOUND_REFERENCE_RECEIVER,
                AsmTypes.OBJECT_TYPE.descriptor
            )
            StackValue.coerce(AsmTypes.OBJECT_TYPE, expectedReceiverType, iv)
        }

fun ClassDescriptor.isSyntheticClassForCallableReference(): Boolean =
        this is SyntheticClassDescriptorForLambda &&
        (this.source as? KotlinSourceElement)?.psi is KtCallableReferenceExpression

fun CalculatedClosure.isForCallableReference(): Boolean =
        closureClass.isSyntheticClassForCallableReference()

fun CalculatedClosure.isForBoundCallableReference(): Boolean =
        isForCallableReference() && capturedReceiverFromOuterContext != null

fun InstructionAdapter.loadBoundReferenceReceiverParameter(index: Int, type: Type) {
    load(index, type)
    StackValue.coerce(type, AsmTypes.OBJECT_TYPE, this)
}

fun CalculatedClosure.isBoundReferenceReceiverField(fieldInfo: FieldInfo): Boolean =
        isForBoundCallableReference() &&
        fieldInfo.fieldName == AsmUtil.CAPTURED_RECEIVER_FIELD

fun InstructionAdapter.generateClosureFieldsInitializationFromParameters(closure: CalculatedClosure, args: List<FieldInfo>): Pair<Int, Type>? {
    var k = 1
    var boundReferenceReceiverParameterIndex = -1
    var boundReferenceReceiverType: Type? = null
    for (fieldInfo in args) {
        if (closure.isBoundReferenceReceiverField(fieldInfo)) {
            boundReferenceReceiverParameterIndex = k
            boundReferenceReceiverType = fieldInfo.fieldType
            k += fieldInfo.fieldType.size
            continue
        }
        k = AsmUtil.genAssignInstanceFieldFromParam(fieldInfo, k, this)
    }

    return when {
        boundReferenceReceiverType != null ->
            Pair(boundReferenceReceiverParameterIndex, boundReferenceReceiverType)
        else ->
            null
    }
}

fun computeExpectedNumberOfReceivers(referencedFunction: FunctionDescriptor, isBound: Boolean): Int {
    val receivers = (if (referencedFunction.dispatchReceiverParameter != null) 1 else 0) +
                    (if (referencedFunction.extensionReceiverParameter != null) 1 else 0) -
                    (if (isBound) 1 else 0)

    if (receivers < 0 && referencedFunction is ConstructorDescriptor &&
               DescriptorUtils.isObject(referencedFunction.containingDeclaration.containingDeclaration)) {
        //reference to object nested class
        //TODO: seems problem should be fixed on frontend side (note that object instance are captured by generated class)
        return 0
    }

    return receivers
}
