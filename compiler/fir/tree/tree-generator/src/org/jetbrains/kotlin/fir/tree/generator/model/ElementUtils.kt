/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.model

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.context.type
import org.jetbrains.kotlin.fir.tree.generator.printer.typeWithArguments

// ----------- Simple field -----------

fun field(name: String, type: String, packageName: String?, customType: Importable? = null, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(name, type, packageName, customType, nullable, withReplace)
}

fun field(name: String, type: Type, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(name, type.typeWithArguments, type.packageName, null, nullable, withReplace)
}

fun field(name: String, typeWithArgs: Pair<Type, List<Importable>>, nullable: Boolean = false, withReplace: Boolean = false): Field {
    val (type, args) = typeWithArgs
    return SimpleField(name, type.typeWithArguments, type.packageName, null, nullable, withReplace).apply {
        arguments += args
    }
}

fun field(type: Type, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return SimpleField(type.type.replaceFirstChar(Char::lowercaseChar), type.typeWithArguments, type.packageName, null, nullable, withReplace)
}

fun booleanField(name: String, withReplace: Boolean = false): Field {
    return field(name, AbstractFirTreeBuilder.boolean, null, withReplace = withReplace)
}

fun stringField(name: String, nullable: Boolean = false): Field {
    return field(name, AbstractFirTreeBuilder.string, null, null, nullable)
}

fun intField(name: String, withReplace: Boolean = false): Field {
    return field(name, AbstractFirTreeBuilder.int, null, withReplace = withReplace)
}

// ----------- Fir field -----------

fun field(name: String, type: Type, argument: String? = null, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return if (argument == null) {
        field(name, type, nullable, withReplace)
    } else {
        field(name, type to listOf(type(argument)), nullable, withReplace)
    }
}

fun field(name: String, element: AbstractElement, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return FirField(name, element, nullable, withReplace)
}

fun field(element: Element, nullable: Boolean = false, withReplace: Boolean = false): Field {
    return FirField(element.name.replaceFirstChar(Char::lowercaseChar), element, nullable, withReplace)
}

// ----------- Field list -----------

fun fieldList(name: String, type: Importable, withReplace: Boolean = false, useMutableOrEmpty: Boolean = false): Field {
    return FieldList(name, type, withReplace, useMutableOrEmpty)
}

fun fieldList(element: AbstractElement, withReplace: Boolean = false, useMutableOrEmpty: Boolean = false): Field {
    return FieldList(element.name.replaceFirstChar(Char::lowercaseChar) + "s", element, withReplace, useMutableOrEmpty)
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