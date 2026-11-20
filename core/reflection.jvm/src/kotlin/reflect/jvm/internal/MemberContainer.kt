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

internal abstract class MemberContainer<T : Any> : KDeclarationContainerImpl(), KClass<T>, KTypeParameterOwnerImpl, TypeConstructorMarker {
    abstract val kmClass: KmClass?

    abstract val classId: ClassId

    abstract val classKind: ClassKind

    abstract val typeParameterTable: TypeParameterTable

    abstract val declaredMemberNames: Set<String>

    abstract val additionalFunctions: Collection<ReflectKCallable<*>>

    abstract fun getDeclaredMembersByName(name: String): Collection<ReflectKCallable<*>>

    abstract fun getMembersByName(name: String): Collection<ReflectKCallable<*>>

    abstract fun getFakeOverrideMembersByName(name: String): MembersJavaSignatureMap
}

internal val MemberContainer<*>.isKotlin: Boolean
    get() = kmClass != null
