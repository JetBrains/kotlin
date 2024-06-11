/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.ElementOrRef as GenericElementOrRef

// ----------- Simple field -----------

fun field(
    name: String,
    type: TypeRefWithNullability,
    nullable: Boolean = false,
    withReplace: Boolean = false,
    withTransform: Boolean = false,
    isChild: Boolean = true,
    initializer: SingleField.() -> Unit = {},
): SingleField {
    val isMutable = type is GenericElementOrRef<*> || withReplace
    return SingleField(
        name,
        type.copy(nullable),
        isChild = isChild,
        isMutable = isMutable,
        withReplace = withReplace,
        withTransform = withTransform
    ).apply(initializer)
}

fun field(
    type: ClassOrElementRef,
    nullable: Boolean = false,
    withReplace: Boolean = false,
    withTransform: Boolean = false,
    isChild: Boolean = true,
    initializer: SingleField.() -> Unit = {},
): SingleField {
    val name = when (type) {
        is ClassRef<*> -> type.simpleName
        is GenericElementOrRef<*> -> type.element.name
    }.replaceFirstChar(Char::lowercaseChar)
    return field(
        name,
        type = type,
        nullable = nullable,
        withReplace = withReplace,
        withTransform = withTransform,
        isChild = isChild,
        initializer = initializer,
    )
}

// ----------- Field list -----------

fun listField(
    name: String,
    baseType: TypeRef,
    withReplace: Boolean = false,
    withTransform: Boolean = false,
    useMutableOrEmpty: Boolean = false,
    isChild: Boolean = true,
    initializer: FieldList.() -> Unit = {},
): Field {
    return FieldList(
        name,
        baseType,
        withReplace = withReplace,
        isChild = isChild,
        useMutableOrEmpty = useMutableOrEmpty,
        withTransform = withTransform,
    ).apply(initializer)
}

fun listField(
    elementOrRef: ElementOrRef,
    withReplace: Boolean = false,
    withTransform: Boolean = false,
    useMutableOrEmpty: Boolean = false,
    isChild: Boolean = true,
    initializer: FieldList.() -> Unit = {},
): Field {
    val name = elementOrRef.element.name.replaceFirstChar(Char::lowercaseChar) + "s"
    return listField(
        name,
        elementOrRef,
        withReplace = withReplace,
        isChild = isChild,
        useMutableOrEmpty = useMutableOrEmpty,
        withTransform = withTransform,
        initializer = initializer,
    )
}

// ----------- Field set -----------

data class FieldSet(val fieldDefinitions: List<Field>) {
    operator fun invoke(config: Field.() -> Unit): FieldSet {
        val configured = fieldDefinitions.map { it.copy().apply(config) }
        return FieldSet(configured)
    }
}

fun fieldSet(vararg fields: Field): FieldSet {
    return FieldSet(fields.toList())
}