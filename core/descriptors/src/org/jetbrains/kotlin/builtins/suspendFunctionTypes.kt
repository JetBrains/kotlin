/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.builtins

import org.jetbrains.kotlin.builtins.StandardNames.CONTINUATION_INTERFACE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.COROUTINES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.impl.MutableClassDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.types.typeUtil.builtIns

private val FAKE_CONTINUATION_CLASS_DESCRIPTOR =
    MutableClassDescriptor(
        EmptyPackageFragmentDescriptor(ErrorUtils.getErrorModule(), COROUTINES_PACKAGE_FQ_NAME),
        ClassKind.INTERFACE, /* isInner = */ false, /* isExternal = */ false,
        CONTINUATION_INTERFACE_FQ_NAME.shortName(), SourceElement.NO_SOURCE, LockBasedStorageManager.NO_LOCKS
    ).apply {
        modality = Modality.ABSTRACT
        visibility = DescriptorVisibilities.PUBLIC
        setTypeParameterDescriptors(
            TypeParameterDescriptorImpl.createWithDefaultBound(
                this, Annotations.EMPTY, false, Variance.IN_VARIANCE, Name.identifier("T"), 0, LockBasedStorageManager.NO_LOCKS
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
            suspendFunType.getContextReceiverTypesFromFunctionType(),
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
