/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.descriptors.ValueClassKind.Inline
import org.jetbrains.kotlin.descriptors.ValueClassKind.MultiField
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.SimpleTypeMarker

sealed class ValueClassRepresentation<Type : SimpleTypeMarker> {
    abstract val underlyingPropertyNamesToTypes: List<Pair<Name, Type>>
    abstract fun containsPropertyWithName(name: Name): Boolean
    abstract fun getPropertyTypeByName(name: Name): Type?

    fun <Other : SimpleTypeMarker> mapUnderlyingType(transform: (Type) -> Other): ValueClassRepresentation<Other> = when (this) {
        is InlineClassRepresentation -> InlineClassRepresentation(underlyingPropertyName, transform(underlyingType))
        is MultiFieldValueClassRepresentation ->
            MultiFieldValueClassRepresentation(underlyingPropertyNamesToTypes.map { (name, type) -> name to transform(type) })
    }
}

enum class ValueClassKind { Inline, MultiField }

fun <Type : SimpleTypeMarker> TypeSystemCommonBackendContext.valueClassLoweringKind(
    fields: List<Pair<Name, Type>>,
): ValueClassKind = when {
    fields.size > 1 -> MultiField
    fields.isEmpty() -> error("Value classes cannot have 0 fields")
    else -> {
        val type = fields.single().second
        with(this) {
            when {
                type.isNullableType() -> Inline
                !type.typeConstructor().isMultiFieldValueClass() -> Inline
                else -> MultiField
            }
        }
    }
}

fun <Type : SimpleTypeMarker> createValueClassRepresentation(context: TypeSystemCommonBackendContext, fields: List<Pair<Name, Type>>) =
    when (context.valueClassLoweringKind(fields)) {
        Inline -> InlineClassRepresentation(fields[0].first, fields[0].second)
        MultiField -> MultiFieldValueClassRepresentation(fields)
    }
