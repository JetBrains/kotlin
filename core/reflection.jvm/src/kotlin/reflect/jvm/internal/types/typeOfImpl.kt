/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.KClassImpl
import kotlin.reflect.jvm.internal.KTypeParameterImpl

internal fun createPlatformKType(lowerBound: KType, upperBound: KType): KType =
    FlexibleKType(lowerBound as AbstractKType, upperBound as AbstractKType)

internal fun createMutableCollectionKType(type: KType): KType {
    val klass = (type as KTypeImpl).classifier

    // Once we get rid of KTypeFromDescriptor, we can remove descriptor from KTypeParameterImpl, and this descriptor-based logic will
    // no longer be needed. To get supertypes and type parameters of a mutable collection class, we'll read the builtins metadata for the
    // class and parse it like this:
    //
    //     val builtins = classLoader.getResourceAsStream("kotlin/collections/collections.kotlin_builtins")!!
    //     val metadata = KotlinCommonMetadata.read(builtins)
    //     ... // Find the needed class, and extract supertypes and type parameters from there.
    require(klass is KClassImpl<*>) {
        "Non-class type cannot be a mutable collection type: $type"
    }
    val mutableDescriptor = klass.descriptor.readOnlyToMutable()

    val mutableCollectionKClass = MutableCollectionKClass(
        klass,
        mutableDescriptor.fqNameUnsafe.asString(),
        mutableDescriptor.typeConstructor.supertypes.map(::KTypeFromDescriptor)
    ) { container ->
        mutableDescriptor.declaredTypeParameters.map { descriptor -> KTypeParameterImpl(container, descriptor) }
    }

    return KTypeImpl(
        type.classifier,
        type.arguments,
        type.isMarkedNullable,
        type.annotations,
        type.abbreviation,
        type.isDefinitelyNotNullType,
        type.isNothingType,
        mutableCollectionKClass,
    )
}

private fun ClassDescriptor.readOnlyToMutable(): ClassDescriptor {
    val fqName = JavaToKotlinClassMap.readOnlyToMutable(fqNameUnsafe)
        ?: throw IllegalArgumentException("Not a readonly collection: $this")
    return builtIns.getBuiltInClassByFqName(fqName)
}

internal fun createNothingType(type: KType): KType {
    type as KTypeImpl
    return KTypeImpl(
        type.classifier,
        type.arguments,
        type.isMarkedNullable,
        type.annotations,
        type.abbreviation,
        type.isDefinitelyNotNullType,
        isNothingType = true,
        type.mutableCollectionClass
    )
}
