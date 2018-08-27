/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.storage.LockBasedStorageManager

private val konanInternalPackageName = FqName("kotlin.native.internal")
private val fakeCapturedTypeClassName = Name.identifier("FAKE_CAPTURED_TYPE_CLASS")

internal fun createFakeClass(packageName: FqName, className: Name)
    = MutableClassDescriptor(
        EmptyPackageFragmentDescriptor(
            ErrorUtils.getErrorModule(), 
            packageName
        ),
        ClassKind.INTERFACE, 
        /* isInner = */ false, 
        /* isExternal = */ false,
        className,
        SourceElement.NO_SOURCE,
        LockBasedStorageManager.NO_LOCKS
    )

// We do the trick similar to FAKE_CONTINUATION_CLASS_DESCRIPTOR.
// To pack a captured type constructor as a type parameter
// of a fake class. We need it because type serialization
// protobuf is not expressive enough.
private val FAKE_CAPTURED_TYPE_CLASS_DESCRIPTOR 
    = createFakeClass(konanInternalPackageName, fakeCapturedTypeClassName)
        .apply {
            modality = Modality.ABSTRACT
            visibility = Visibilities.PUBLIC
            setTypeParameterDescriptors(
                TypeParameterDescriptorImpl
                    .createWithDefaultBound(
                        this, Annotations.EMPTY, 
                        false, Variance.IN_VARIANCE, 
                        Name.identifier("Projection"), 
                        /* index = */ 0
                    ).let(::listOf)
            )
            createTypeConstructor()
        }

internal fun packCapturedType(type: CapturedType): SimpleType =
    KotlinTypeFactory.simpleType(
        Annotations.EMPTY,
        FAKE_CAPTURED_TYPE_CLASS_DESCRIPTOR.typeConstructor,
        listOf(type.typeProjection), 
        nullable = false
    )

internal fun unpackCapturedType(type: KotlinType)
    = createCapturedType(type.arguments.single())
