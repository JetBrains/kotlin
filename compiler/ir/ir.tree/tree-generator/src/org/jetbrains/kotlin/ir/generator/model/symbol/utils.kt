/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model.symbol

import org.jetbrains.kotlin.generators.tree.AbstractField.SymbolFieldRole
import org.jetbrains.kotlin.generators.tree.ClassOrElementRef
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.classifierSymbol
import org.jetbrains.kotlin.ir.generator.IrSymbolTree.typeAliasSymbol
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeAbbreviationType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly

fun symbolRemapperMethodName(symbolClass: Symbol, role: SymbolFieldRole): String {
    val elementName = symbolClass.name.removeSuffix("Symbol")
    return "get${role.name.lowercase().capitalizeAsciiOnly()}$elementName"
}

data class FieldWithSymbol(
    val symbolType: Symbol,
    val fieldName: String,
    val role: SymbolFieldRole,
    val fieldContainer: ClassOrElementRef,
)

private val additionalSymbolFields = listOf(
    FieldWithSymbol(classifierSymbol, "classifier", SymbolFieldRole.REFERENCED, irSimpleTypeType),
    FieldWithSymbol(typeAliasSymbol, "typeAlias", SymbolFieldRole.REFERENCED, irTypeAbbreviationType)
)

private val Element.fieldsWithSymbols: List<FieldWithSymbol>
    get() = allFields.mapNotNull { field ->
        val role = field.symbolFieldRole ?: return@mapNotNull null
        val symbolClass = field.symbolClass ?: return@mapNotNull null
        FieldWithSymbol(symbolClass, field.name, role, this)
    }

fun findFieldsWithSymbols(elements: List<Element>, role: SymbolFieldRole): Map<Symbol, List<FieldWithSymbol>> {
    val elementSymbolFields = elements.flatMap { element ->
        if (element.implementations.isNotEmpty()) {
            element.fieldsWithSymbols
        } else {
            emptyList()
        }
    }
    return (elementSymbolFields + additionalSymbolFields)
        .filter { it.role == role }
        .groupBy { it.symbolType }
}