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
 * - [InlineClassRepresentation] — a single-field value class which is inline: declared as `inline value class`, `@JvmInline value class`,
 *   the deprecated `inline class`, or as a plain `value class` on Common or a non-JVM target with `FullValueClasses` disabled
 *   ([KEEP-0104](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0104-inline-classes.md)).
 *   It is unboxed by the compiler on all backends where possible.
 * - [FullValueClassRepresentation] — a `value class` with neither the `inline` modifier nor the `@JvmInline` annotation,
 *   or a `value object`, when the `FullValueClasses` language feature is enabled
 *   ([KEEP-0454](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0454-better-immutability-value-classes-MFVC.md)).
 *   It is never interpreted as an inline class: no backend unboxes it the way inline classes are unboxed, even if it has a single field.
 *     * Full value class can have zero (a `value object`), one or multiple underlying fields, and a (non-`Any`) super class.
 *     * Full value class can be abstract/sealed.
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


@RequiresOptIn("Use backend-specific APIs instead of this one. See usages for examples.")
annotation class ValueClassBackendAgnosticApi
