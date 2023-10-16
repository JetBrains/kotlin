/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.generators.tree.*

// ----------- Simple field -----------

fun field(name: String, type: TypeRefWithNullability, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(name, type.copy(nullable), withReplace)
}

fun field(type: ClassRef<*>, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(type.simpleName.replaceFirstChar(Char::lowercaseChar), type.copy(nullable), withReplace)
}

fun booleanField(name: String, withReplace: Boolean = false): Field {
    return field(name, StandardTypes.boolean, withReplace = withReplace)
}

fun stringField(name: String, nullable: Boolean = false): Field {
    return field(name, StandardTypes.string, nullable = nullable)
}

fun intField(name: String, withReplace: Boolean = false): Field {
    return field(name, StandardTypes.int, withReplace = withReplace)
}

// ----------- Fir field -----------

fun field(name: String, element: ElementOrRef, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return FirField(name, element.copy(nullable), withReplace)
}

fun field(element: Element, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return FirField(element.name.replaceFirstChar(Char::lowercaseChar), element.copy(nullable), withReplace)
}

// ----------- Field list -----------

fun fieldList(name: String, type: TypeRef, withReplace: Boolean = false, useMutableOrEmpty: Boolean = false): Field {
    return FieldList(name, type, withReplace, useMutableOrEmpty)
}

fun fieldList(elementOrRef: ElementOrRef, withReplace: Boolean = false, useMutableOrEmpty: Boolean = false): Field {
    return FieldList(elementOrRef.element.name.replaceFirstChar(Char::lowercaseChar) + "s", elementOrRef, withReplace, useMutableOrEmpty)
}

// ----------- Field set -----------

typealias FieldSet = List<Field>

fun fieldSet(vararg fields: Field): FieldSet {
    return fields.toList()
}

@JvmName("foo")
infix fun FieldSet.with(sets: List<FieldSet>): FieldSet {
    return sets.flatten()
}

infix fun FieldSet.with(set: FieldSet): FieldSet {
    return this + set
}

fun Field.withTransform(needTransformInOtherChildren: Boolean = false): Field = copy().apply {
    needsSeparateTransform = true
    this.needTransformInOtherChildren = needTransformInOtherChildren
}

fun Field.withReplace(): Field = copy().apply {
    withReplace = true
}
