/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.KmClass
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.jvm.internal.*

/**
 * A [KClass] implementation for mutable collection classes (i.e. `kotlin.collections.MutableList`).
 *
 * Currently, this class is only used in the type checker implementation for kotlin-reflect,
 * but one day it should probably be used to implement KT-11754.
 *
 * @property readonlyClass the read-only collection class (i.e. `kotlin.collections.List`)
 */
internal interface MutableCollectionKClass<T : Any> : KClass<T>, TypeConstructorMarker, KTypeParameterOwnerImpl {
    val readonlyClass: KClass<T>

    val mutableKmClass: KmClass?
}

internal class MutableCollectionKClassImpl<T : Any>(
    override val readonlyClass: KClassImpl<T>,
    val mutableClassId: ClassId,
) : KClass<T> by readonlyClass, MutableCollectionKClass<T> {
    override val qualifiedName: String
        get() = mutableClassId.asSingleFqName().asString()

    override val simpleName: String
        get() = mutableClassId.shortClassName.asString()

    override val mutableKmClass: KmClass by lazy(PUBLICATION) {
        readBuiltinClassMetadata(mutableClassId)
            ?: throw KotlinReflectionInternalError("Builtin class metadata not found for $mutableClassId.")
    }

    private val typeParameterTable: TypeParameterTable by lazy(PUBLICATION) {
        TypeParameterTable.create(mutableKmClass.typeParameters, parent = null, readonlyClass, readonlyClass.java.safeClassLoader)
    }

    override val typeParameters: List<KTypeParameter>
        get() = typeParameterTable.ownTypeParameters

    override val supertypes: List<KType> by lazy(PUBLICATION) {
        mutableKmClass.supertypes.map {
            it.toKType(readonlyClass.java.safeClassLoader, typeParameterTable)
        }
    }

    override fun equals(other: Any?): Boolean = other is MutableCollectionKClass<*> && readonlyClass == other.readonlyClass
    override fun hashCode(): Int = readonlyClass.hashCode()
    override fun toString(): String = "MutableCollectionKClass($readonlyClass)"
}

internal class DescriptorMutableCollectionKClass<T : Any>(
    override val readonlyClass: KClass<T>,
    val mutableClassDescriptor: ClassDescriptor,
) : KClass<T> by readonlyClass, MutableCollectionKClass<T> {
    override val qualifiedName: String
        get() = mutableClassDescriptor.fqNameSafe.asString()

    override val simpleName: String
        get() = mutableClassDescriptor.name.asString()

    override val mutableKmClass: KmClass?
        get() = null

    override val typeParameters: List<KTypeParameter> by lazy(PUBLICATION) {
        mutableClassDescriptor.declaredTypeParameters.map { descriptor -> KTypeParameterImpl(this, descriptor) }
    }

    override val supertypes: List<KType> by lazy(PUBLICATION) {
        mutableClassDescriptor.typeConstructor.supertypes.map(::DescriptorKType)
    }

    override fun equals(other: Any?): Boolean = other is MutableCollectionKClass<*> && readonlyClass == other.readonlyClass
    override fun hashCode(): Int = readonlyClass.hashCode()
    override fun toString(): String = "MutableCollectionKClass($readonlyClass)"
}
