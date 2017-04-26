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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType

/**
 * Value type is a kind of Kotlin types represented as plain value in generated code, not as an object reference.
 * Such types may require autoboxing.
 *
 * The value type nearly corresponds to `[classFqName]` or `[classFqName]?` Kotlin type (depending on [isNullable]).
 *
 * @property classFqName name of the base value type class
 * @property isNullable whether `null` is included into this value type
 */
enum class ValueType(val classFqName: FqNameUnsafe, val isNullable: Boolean = false) {
    BOOLEAN(KotlinBuiltIns.FQ_NAMES._boolean),
    CHAR(KotlinBuiltIns.FQ_NAMES._char),

    BYTE(KotlinBuiltIns.FQ_NAMES._byte),
    SHORT(KotlinBuiltIns.FQ_NAMES._short),
    INT(KotlinBuiltIns.FQ_NAMES._int),
    LONG(KotlinBuiltIns.FQ_NAMES._long),
    FLOAT(KotlinBuiltIns.FQ_NAMES._float),
    DOUBLE(KotlinBuiltIns.FQ_NAMES._double),

    UNBOUND_CALLABLE_REFERENCE(FqNameUnsafe("konan.internal.UnboundCallableReference")),
    NATIVE_PTR(KonanBuiltIns.FqNames.nativePtr),

    NATIVE_POINTED(InteropBuiltIns.FqNames.nativePointed, isNullable = true),
    C_POINTER(InteropBuiltIns.FqNames.cPointer, isNullable = true)
}

private fun KotlinType.isConstructedFromGivenClass(fqName: FqNameUnsafe) =
        KotlinBuiltIns.isConstructedFromGivenClass(this, fqName)

/**
 * @return `true` if this type must be represented as given value type in generated code.
 */
tailrec fun KotlinType.isRepresentedAs(valueType: ValueType): Boolean {
    if (this.isMarkedNullable && !valueType.isNullable) {
        return false
    }

    if (this.isConstructedFromGivenClass(valueType.classFqName)) {
        return true
    }

    // Supertypes should be checked even for "final" value types (e.g. Int)
    // to treat type parameter `T` with `Int` upper bound as value type.
    // This behavior is observed on Kotlin JVM and used in interop implementation.
    //
    // However to optimize this method only first supertype is checked
    // (it is supposed to be enough in all sane cases).
    val firstSupertype = this.constructor.supertypes.firstOrNull() ?: return false
    return firstSupertype.isRepresentedAs(valueType)
}

/**
 * @return `true` if this type without `null` value must be represented as given value type in generated code.
 *
 * TODO: this method can be considered as a hack; rework its usages.
 */
tailrec fun KotlinType.notNullableIsRepresentedAs(valueType: ValueType): Boolean {
    if (this.isConstructedFromGivenClass(valueType.classFqName)) {
        return true
    }

    // See comment in [isRepresentedAs].
    val firstSupertype = this.constructor.supertypes.firstOrNull() ?: return false
    return firstSupertype.notNullableIsRepresentedAs(valueType)
}

internal fun KotlinType.isValueType() = ValueType.values().any { this.isRepresentedAs(it) }
