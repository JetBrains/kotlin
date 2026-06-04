/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.descriptors.ValueClassKind.Inline
import org.jetbrains.kotlin.descriptors.ValueClassKind.MultiField
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.RigidTypeMarker
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

/**
 * Represents how a value class is lowered/unboxed by the compiler.
 *
 * There are three possible representations:
 * - [InlineClassRepresentation] — a single-field value class declared with `inline` keyword or `@JvmInline` annotation and `value` keyword
 *   ([KEEP-0104](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0104-inline-classes.md))
 *   It is always unboxed by the compiler on all backends.
 * - [JvmInlineMultiFieldValueClassRepresentation] — a multi-field `@JvmInline value class`
 *   ([KEEP-0340](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0340-multi-field-value-classes.md)).
 *   It is unboxed (flattened into multiple fields) by the compiler on JVM. Not available on other backends.
 * - [FullValueClassRepresentation] — a value class without `@JvmInline` annotation
 *   ([KEEP-0454](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0454-better-immutability-value-classes-MFVC.md)).
 *   It is not unboxed on JVM, but on other backends a single-field full value class is treated the same as an inline class and thus is unboxed.
 *   It can have multiple underlying fields.
 *
 * [InlineClassRepresentation] and [JvmInlineMultiFieldValueClassRepresentation] are grouped under [BasicValueClassRepresentation].
 */
sealed class ValueClassRepresentation<Type : RigidTypeMarker> {
    abstract val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>?
    abstract fun containsPropertyWithName(name: Name): Boolean
    abstract fun getPropertyTypeByName(name: Name): Type?

    fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): ValueClassRepresentation<Other> = when (this) {
        is InlineClassRepresentation -> InlineClassRepresentation(underlyingPropertyName, transform(underlyingType))
        is JvmInlineMultiFieldValueClassRepresentation ->
            JvmInlineMultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes.map { [name, type] -> name to transform(type) })
        is FullValueClassRepresentation ->
            FullValueClassRepresentation(underlyingPropertyNamesToTypes?.map { [name, type] -> name to transform(type) })
    }
}

sealed class BasicValueClassRepresentation<Type : RigidTypeMarker>: ValueClassRepresentation<Type>() {
    abstract override val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
}

fun <T : RigidTypeMarker> ValueClassRepresentation<T>.asOld() = this as? BasicValueClassRepresentation

enum class ValueClassKind { Inline, MultiField }

fun <Type : RigidTypeMarker> TypeSystemCommonBackendContext.valueClassLoweringKind(
    fields: List<Pair<Name, Type>>,
): ValueClassKind = when {
    fields.size > 1 -> MultiField
    fields.isEmpty() -> error("Value classes cannot have 0 fields")
    else -> {
        val type = fields.single().second
        with(this) {
            when {
                type.isNullableType() -> Inline
                !type.typeConstructor().isJvmInlineMultiFieldValueClass() -> Inline
                else -> MultiField
            }
        }
    }
}

fun <Type : RigidTypeMarker> createValueClassRepresentation(context: TypeSystemCommonBackendContext, fields: List<Pair<Name, Type>>) =
    when (context.valueClassLoweringKind(fields)) {
        Inline -> InlineClassRepresentation(fields[0].first, fields[0].second)
        MultiField -> JvmInlineMultiFieldValueClassRepresentation(fields)
    }



/**
 * Casts or converts the [ValueClassRepresentation] to [InlineClassRepresentation] depending on the [treatFullValueClassesWithOneFieldAsBasic],
 * which specifies whether a single-field full value class is compatible with being an inline class or not.
 *
 * **Full** value classes are value classes described in [this KEEP](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0454-better-immutability-value-classes-MFVC.md).
 *
 * **Basic** value classes are [inline classes](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0104-inline-classes.md) and [jvm inline multi-field value classes](https://github.com/Kotlin/KEEP/blob/main/proposals/KEEP-0340-multi-field-value-classes.md)
 *
 * The overview of full value classes is that they are value classes without @JvmInline annotation on all backends, supporting one or multiple underlying fields.
 *
 * They are not optimized on JVM, regardless of the number of underlying fields. On other backends, they are optimized if there is only one underlying field.
 *
 * @param treatFullValueClassesWithOneFieldAsBasic A boolean indicating whether to treat full value classes with one underlying field as basic (inline class).
 *                                                 On JVM full value classes are not unboxed on the behalf of Kotlin compiler while `inline class`es/`@JvmInline value class`es are.
 *                                                 On other platforms there is no `@JvmInline` annotation and unboxing is done by the compiler in both basic and full value classes with a single field.
 *                                                 Therefore, full value classes with one field are actually preexisting value classes on other platforms.
 *                                                 `false` must be used for JVM, `true` for other backends.
 * @return An [InlineClassRepresentation] if the class has a compatible value class
 *         representation and meets the conditions specified by the [treatFullValueClassesWithOneFieldAsBasic]
 *         parameter; otherwise, `null`.
 */
@ValueClassBackendAgnosticApi
fun <T : RigidTypeMarker> ValueClassRepresentation<T>.interpretAsInlineClassRepresentationOrNull(
    treatFullValueClassesWithOneFieldAsBasic: Boolean
): InlineClassRepresentation<T>? = when (this) {
    is InlineClassRepresentation -> this
    is JvmInlineMultiFieldValueClassRepresentation -> null
    is FullValueClassRepresentation if !treatFullValueClassesWithOneFieldAsBasic -> null
    is FullValueClassRepresentation -> underlyingPropertyNamesToTypes?.singleOrNull()
        ?.let { [name, type] -> InlineClassRepresentation(name, type) }
}

@RequiresOptIn("Use backend-specific APIs instead of this one. See usages for examples.")
annotation class ValueClassBackendAgnosticApi
