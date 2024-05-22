/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model.symbol

import org.jetbrains.kotlin.generators.tree.AbstractField
import org.jetbrains.kotlin.generators.tree.AbstractField.Kind
import org.jetbrains.kotlin.generators.tree.ClassOrElementRef
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.classifierSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.typeAliasSymbol
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeAbbreviationType
import org.jetbrains.kotlin.ir.generator.model.Element

fun symbolRemapperMethodName(symbolClass: Symbol, kind: Kind): String {
    val elementName = symbolClass.name.removeSuffix("Symbol")
    val kindDesc = when(kind) {
        AbstractField.Kind.DeclaredSymbol -> "Declared"
        AbstractField.Kind.ElementReference -> "Referenced"
        else -> error("Unexpected kind $kind")
    }
    return "get$kindDesc$elementName"
}

data class FieldWithSymbol(
    val symbolType: Symbol,
    val fieldName: String,
    val kind: Kind,
    val fieldContainer: ClassOrElementRef,
)

private val additionalSymbolFields = listOf(
    FieldWithSymbol(classifierSymbol, "classifier", Kind.ElementReference, irSimpleTypeType),
    FieldWithSymbol(typeAliasSymbol, "typeAlias", Kind.ElementReference, irTypeAbbreviationType)
)

private val Element.fieldsWithSymbols: List<FieldWithSymbol>
    get() = allFields.mapNotNull { field ->
        val symbolClass = field.symbolClass ?: return@mapNotNull null
        FieldWithSymbol(symbolClass, field.name, field.kind, this)
    }

fun findFieldsWithSymbols(elements: List<Element>, kind: Kind): Map<Symbol, List<FieldWithSymbol>> {
    val elementSymbolFields = elements.flatMap { element ->
        if (element.implementations.isNotEmpty()) {
            element.fieldsWithSymbols
        } else {
            emptyList()
        }
    }
    return (elementSymbolFields + additionalSymbolFields)
        .filter { it.kind == kind }
        .groupBy { it.symbolType }
}