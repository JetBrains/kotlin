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

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns


val FAKE_CONTINUATION_CLASS_DESCRIPTOR =
        MutableClassDescriptor(
                EmptyPackageFragmentDescriptor(ErrorUtils.getErrorModule(), DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME),
                ClassKind.INTERFACE, /* isInner = */ false, /* isExternal = */ false,
                DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME.shortName(), SourceElement.NO_SOURCE
        ).apply {
            modality = Modality.ABSTRACT
            visibility = Visibilities.PUBLIC
            setTypeParameterDescriptors(
                    TypeParameterDescriptorImpl.createWithDefaultBound(
                            this, Annotations.EMPTY, false, Variance.IN_VARIANCE, Name.identifier("T"), 0
                    ).let(::listOf)
            )
            createTypeConstructor()
        }


fun transformSuspendFunctionToRuntimeFunctionType(suspendFunType: KotlinType): SimpleType {
    assert(suspendFunType.isSuspendFunctionType) {
        "This type should be suspend function type: $suspendFunType"
    }

    return createFunctionType(
            suspendFunType.builtIns,
            suspendFunType.annotations,
            suspendFunType.getReceiverTypeFromFunctionType(),
            suspendFunType.getValueParameterTypesFromFunctionType().map(TypeProjection::getType) +
            KotlinTypeFactory.simpleType(
                    Annotations.EMPTY,
                    // Continuation interface is not a part of built-ins anymore, it has been moved to stdlib.
                    // While it must be somewhere in the dependencies, but here we don't have a reference to the module,
                    // and it's rather complicated to inject it by now, so we just use a fake class descriptor.
                    FAKE_CONTINUATION_CLASS_DESCRIPTOR.typeConstructor,
                    listOf(suspendFunType.getReturnTypeFromFunctionType().asTypeProjection()), nullable = false
            ),
            // TODO: names
            null,
            suspendFunType.builtIns.nullableAnyType
    ).makeNullableAsSpecified(suspendFunType.isMarkedNullable)
}

fun transformRuntimeFunctionTypeToSuspendFunction(funType: KotlinType): SimpleType? {
    assert(funType.isFunctionType) {
        "This type should be function type: $funType"
    }

    val continuationArgumentType = funType.getValueParameterTypesFromFunctionType().lastOrNull()?.type ?: return null
    if (continuationArgumentType.constructor.declarationDescriptor?.fqNameSafe != DescriptorUtils.CONTINUATION_INTERFACE_FQ_NAME
        || continuationArgumentType.arguments.size != 1
    ) {
        return null
    }

    val suspendReturnType = continuationArgumentType.arguments.single().type

    return createFunctionType(
            funType.builtIns,
            funType.annotations,
            funType.getReceiverTypeFromFunctionType(),
            funType.getValueParameterTypesFromFunctionType().dropLast(1).map(TypeProjection::getType),
            // TODO: names
            null,
            suspendReturnType,
            suspendFunction = true
    ).makeNullableAsSpecified(funType.isMarkedNullable)
}