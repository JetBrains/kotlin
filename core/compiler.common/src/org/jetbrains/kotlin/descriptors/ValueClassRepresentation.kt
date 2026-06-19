/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

/**
 * Represents how a value class is lowered/unboxed by the compiler.
 *
 * There are two possible representations:
 * - [InlineClassRepresentation] — a single-field value class declared with `inline` keyword or `@JvmInline` annotation and `value` keyword
 *   ([KEEP-0104](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0104-inline-classes.md))
 *   It is always unboxed by the compiler on all backends.
 * - [FullValueClassRepresentation] — a value class without `@JvmInline` annotation with `value` keyword
 *   ([KEEP-0454](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0454-better-immutability-value-classes-MFVC.md)).
 *     * Full value class can have one field and no (non-`Any`) super class.
 *       * In the case of non-JVM platforms, it looks like an inline class defined above and thus behaves correspondingly: it gets unboxed. It is considered **COMPATIBLE** with an inline class.
 *       * On JVM the class is not unboxed, as inline classes have [JvmInline] annotation. It is considered **INCOMPATIBLE** with an inline class.
 *     * Full value class can have one field and a (non-`Any`) super class. In this case it is always boxed and thus is considered **INCOMPATIBLE** with an inline class.
 *     * Full value class can have multiple underlying fields. In this case it is always boxed and thus is considered **INCOMPATIBLE** with an inline class.
 *     * Full value class can be abstract/sealed. It is considered **INCOMPATIBLE** with an inline class.
 *       * It is a regular abstract class without any fields (if declared in Kotlin) and without mutable fields (if declared in Java with Valhalla support).
 *       * It can be inherited by other value classes (both abstract/sealed and final).
 *       * Its [underlyingPropertyNamesToTypes] is always `null`.
 */
sealed class ValueClassRepresentation<Type : RigidTypeMarker> {
    abstract val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>?
    abstract fun containsPropertyWithName(name: Name): Boolean
    abstract fun getPropertyTypeByName(name: Name): Type?

    fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): ValueClassRepresentation<Other> = when (this) {
        is InlineClassRepresentation -> InlineClassRepresentation(underlyingPropertyName, transform(underlyingType))
        is FullValueClassRepresentation ->
            FullValueClassRepresentation(underlyingPropertyNamesToTypes?.map { [name, type] -> name to transform(type) })
    }
}


/**
 * Interprets given [ValueClassRepresentation] as [InlineClassRepresentation].
 *
 * * If the actual given [ValueClassRepresentation] is an instance of [InlineClassRepresentation], the function simply returns it.
 * * If [treatCompatibleFullValueClassesAsInline] is `true` and the [ValueClassRepresentation] is a compatible [FullValueClassRepresentation],
 *   the function computes and returns the compatible [InlineClassRepresentation].
 *
 * [hasSuperClass] is called only when the function result is ambiguous without that.
 *
 * See [ValueClassRepresentation] documentation for more details about value class types and their compatibility.
 *
 * @return An [InlineClassRepresentation] or `null`.
 */
@ValueClassBackendAgnosticApi
fun <T : RigidTypeMarker> ValueClassRepresentation<T>.interpretAsInlineClassRepresentationOrNull(
    treatCompatibleFullValueClassesAsInline: Boolean,
    hasSuperClass: () -> Boolean,
): InlineClassRepresentation<T>? = when (this) {
    is InlineClassRepresentation -> this
    is FullValueClassRepresentation if !treatCompatibleFullValueClassesAsInline -> null
    is FullValueClassRepresentation -> underlyingPropertyNamesToTypes?.singleOrNull()
        ?.takeIf { !hasSuperClass() }
        ?.let { [name, type] -> InlineClassRepresentation(name, type) }
}

@RequiresOptIn("Use backend-specific APIs instead of this one. See usages for examples.")
annotation class ValueClassBackendAgnosticApi
