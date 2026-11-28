/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.types

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.runtime.structure.safeClassLoader
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import java.util.concurrent.ConcurrentHashMap
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.reflect.KCallable
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
) : MemberContainer<T>(), MutableCollectionKClass<T>, KClass<T> by readonlyClass {
    override val jClass: Class<T>
        get() = readonlyClass.jClass

    override val classId: ClassId
        get() = mutableClassId

    override val classKind: ClassKind
        get() = readonlyClass.classKind

    override val qualifiedName: String
        get() = mutableClassId.asSingleFqName().asString()

    override val simpleName: String
        get() = mutableClassId.shortClassName.asString()

    override val mutableKmClass: KmClass by lazy(PUBLICATION) {
        readBuiltinClassMetadata(mutableClassId)
            ?: throw KotlinReflectionInternalError("Builtin class metadata not found for $mutableClassId.")
    }

    override val kmClass: KmClass
        get() = mutableKmClass

    override val typeParameterTable: TypeParameterTable by lazy(PUBLICATION) {
        TypeParameterTable.create(mutableKmClass.typeParameters, parent = null, this, readonlyClass.java.safeClassLoader)
    }

    override val typeParameters: List<KTypeParameter>
        get() = typeParameterTable.ownTypeParameters

    override val supertypes: List<KType> by lazy(PUBLICATION) {
        mutableKmClass.supertypes.map {
            it.toKType(readonlyClass.java.safeClassLoader, typeParameterTable)
        }
    }

    override val functionsMetadata get() = mutableKmClass.functions
    override val propertiesMetadata get() = mutableKmClass.properties
    override val constructorsMetadata get() = mutableKmClass.constructors
    override val constructorDescriptors: Collection<ConstructorDescriptor> get() = emptyList()

    override fun getProperties(name: Name): Collection<PropertyDescriptor> = emptyList()
    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = emptyList()
    override fun getLocalPropertyDescriptor(index: Int): PropertyDescriptor? = null
    override fun getLocalPropertyMetadata(index: Int) = null

    override val declaredMemberNames: Set<String> by lazy(PUBLICATION, ::computeDeclaredMemberNamesFromMetadata)

    override val additionalFunctions: Collection<ReflectKCallable<*>> by lazy(PUBLICATION, ::getAdditionalFunctions)

    override val members: Collection<KCallable<*>> by lazy(PUBLICATION, ::computeAllMembers)

    override fun getDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>> =
        computeDeclaredMembersFromMetadata(name)

    override fun getMembersByName(name: String): Collection<ReflectKCallable<*>> = computeMembersByName(name)

    private val fakeOverrideMembersByName = ConcurrentHashMap<String, MembersJavaSignatureMap>()

    override fun getFakeOverrideMembersByName(name: String): MembersJavaSignatureMap =
        fakeOverrideMembersByName.getOrPut(name) { computeFakeOverrideMembersForName(this, name) }

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
    override fun hashCode(): Int = readonlyClass.hashCode() * 31
    override fun toString(): String = "MutableCollectionKClass($readonlyClass)"
}
