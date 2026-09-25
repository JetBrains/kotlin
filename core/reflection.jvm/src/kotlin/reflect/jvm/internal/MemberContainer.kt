/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.types.model.TypeConstructorMarker
import kotlin.metadata.ClassKind
import kotlin.metadata.KmClass
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.types.AbstractKType

/**
 * A class-like declaration container which has members: declared members (functions and properties loaded from metadata or Java
 * reflection), members inherited from supertypes (fake overrides), and additional functions of mapped built-in classes.
 *
 * All member computation algorithms (see `KClassMembers.kt` and `fakeOverrides.kt`) are written against this abstraction, so that they
 * can be reused not only for [KClassImpl], but also for other member-bearing classes, which are not backed by a Java class directly.
 */
internal abstract class MemberContainer<T : Any> : KDeclarationContainerImpl(), KClass<T>, KTypeParameterOwnerImpl, TypeConstructorMarker {
    abstract val kmClass: KmClass?

    abstract val classId: ClassId

    abstract val classKind: ClassKind

    abstract val typeParameterTable: TypeParameterTable

    /**
     * Names of all members declared in this container, including additional functions of mapped built-in classes.
     */
    abstract val declaredMemberNames: Set<String>

    /**
     * Java methods of a built-in class's Java analogue that should be visible on the Kotlin class but are not declared in its metadata,
     * see `getAdditionalFunctions`.
     */
    abstract val additionalFunctions: Collection<ReflectKCallable<*>>

    abstract fun getDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>>

    abstract fun getMembersByName(name: String): Collection<ReflectKCallable<*>>

    abstract fun getFakeOverrideMembersByName(name: String): MembersJavaSignatureMap
}

internal val MemberContainer<*>.isKotlin: Boolean
    get() = kmClass != null

/**
 * The container of members of this type's class, when the type is used as a supertype. For mutable collection types (e.g. `MutableList<E>`
 * or the mutability-flexible `(Mutable)List<E!>`), it's the mutable collection class, whose members differ from the members of the
 * read-only class which is the [KType.classifier] of such types.
 */
internal val KType.memberContainer: MemberContainer<*>?
    get() = (this as? AbstractKType)?.mutableCollectionClass as? MemberContainer<*> ?: classifier as? MemberContainer<*>
