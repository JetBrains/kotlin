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
import kotlin.metadata.*
import kotlin.reflect.*
import kotlin.reflect.jvm.internal.*

/**
 * A [KClass] implementation for mutable collection classes (i.e. `kotlin.collections.MutableList`).
 *
 * Currently, this class is used in the type checker implementation for kotlin-reflect, and as a container of members of the mutable
 * collection class (as opposed to the read-only class, i.e. `kotlin.collections.List`), so that subclasses of mutable collections
 * could inherit these members. It's not exposed to the user via the public API yet, but one day it should probably be used to implement
 * KT-11754.
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
) : MemberContainer<T>(), MutableCollectionKClass<T> {
    override val jClass: Class<T>
        get() = readonlyClass.jClass

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

    override val classId: ClassId
        get() = mutableClassId

    override val classKind: ClassKind
        get() = mutableKmClass.kind

    // Type parameters of the mutable class are intentionally created with the read-only class as the container, so that they are equal to
    // the type parameters of the read-only class (see `KTypeParameterImpl.equals`). This is relied upon when resolving Java type variables
    // of the Java analogue (e.g. `E` of `java.util.List`) and when enhancing mutability of collection types.
    override val typeParameterTable: TypeParameterTable by lazy(PUBLICATION) {
        TypeParameterTable.create(mutableKmClass.typeParameters, parent = null, readonlyClass, readonlyClass.java.safeClassLoader)
    }

    override val typeParameters: List<KTypeParameter>
        get() = typeParameterTable.ownTypeParameters

    override val supertypes: List<KType> by lazy(PUBLICATION) {
        mutableKmClass.supertypes.map {
            it.toKType(readonlyClass.java.safeClassLoader, typeParameterTable)
        }
    }

    private val declaredMembersByName = ConcurrentHashMap<String, Collection<ReflectKCallable<*>>>()

    private val allMembersByName = ConcurrentHashMap<String, Collection<ReflectKCallable<*>>>()

    private val fakeOverrideMembersByName = ConcurrentHashMap<String, MembersJavaSignatureMap>()

    override val declaredMemberNames: Set<String> by lazy(PUBLICATION) { computeDeclaredMemberNamesFromMetadata() }

    override val additionalFunctions: Collection<ReflectKCallable<*>> by ReflectProperties.lazySoft { getAdditionalFunctions() }

    override fun getDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>> =
        declaredMembersByName.getOrPut(name) { computeDeclaredMembersFromMetadata(name) }

    override fun getMembersByName(name: String): Collection<ReflectKCallable<*>> =
        allMembersByName.getOrPut(name) { computeMembersByName(name) }

    override fun getFakeOverrideMembersByName(name: String): MembersJavaSignatureMap =
        fakeOverrideMembersByName.getOrPut(name) { computeFakeOverrideMembersForName(this, name) }

    override val members: Collection<KCallable<*>> by lazy(PUBLICATION) { computeAllMembers() }

    override val functionsMetadata: Collection<KmFunction>
        get() = mutableKmClass.functions

    override val propertiesMetadata: Collection<KmProperty>
        get() = mutableKmClass.properties

    override val constructorsMetadata: Collection<KmConstructor>
        get() = emptyList()

    override val constructorDescriptors: Collection<ConstructorDescriptor>
        get() = emptyList()

    override fun getProperties(name: Name): Collection<PropertyDescriptor> = readonlyClass.getProperties(name)

    override fun getFunctions(name: Name): Collection<FunctionDescriptor> = readonlyClass.getFunctions(name)

    override fun getLocalPropertyDescriptor(index: Int): PropertyDescriptor? = null

    override fun getLocalPropertyMetadata(index: Int): KmProperty? = null

    // All the remaining KClass members which are not related to members of this class are delegated to the read-only class.

    override val annotations: List<Annotation> get() = readonlyClass.annotations
    override val constructors: Collection<KFunction<T>> get() = readonlyClass.constructors
    override val nestedClasses: Collection<KClass<*>> get() = readonlyClass.nestedClasses
    override val objectInstance: T? get() = readonlyClass.objectInstance
    override val sealedSubclasses: List<KClass<out T>> get() = readonlyClass.sealedSubclasses
    override val visibility: KVisibility? get() = readonlyClass.visibility
    override val isFinal: Boolean get() = readonlyClass.isFinal
    override val isOpen: Boolean get() = readonlyClass.isOpen
    override val isAbstract: Boolean get() = readonlyClass.isAbstract
    override val isSealed: Boolean get() = readonlyClass.isSealed
    override val isData: Boolean get() = readonlyClass.isData
    override val isInner: Boolean get() = readonlyClass.isInner
    override val isCompanion: Boolean get() = readonlyClass.isCompanion
    override val isFun: Boolean get() = readonlyClass.isFun
    override val isValue: Boolean get() = readonlyClass.isValue

    override fun isInstance(value: Any?): Boolean = readonlyClass.isInstance(value)

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
