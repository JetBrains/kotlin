/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print.symbol

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol
import org.jetbrains.kotlin.ir.generator.model.symbol.SymbolField
import org.jetbrains.kotlin.ir.generator.model.symbol.findFieldsWithSymbols

internal class SymbolPrinter(
    printer: ImportCollectingPrinter,
    model: Model<Element>,
) : AbstractElementPrinter<Symbol, SymbolField>(printer) {

    private val fieldsWithReferencedSymbols = findFieldsWithSymbols(model.elements, AbstractField.SymbolFieldRole.REFERENCED)

    override fun makeFieldPrinter(printer: ImportCollectingPrinter) = object : AbstractFieldPrinter<SymbolField>(printer) {
        override fun forceMutable(field: SymbolField): Boolean = field.isMutable
    }

    override val separateFieldsWithBlankLine: Boolean
        get() = true

    override fun ImportCollecting.elementKDoc(element: Symbol): String = buildString {
        val owners = if (element.isSealed) {
            element.subElements.mapNotNull { it.owner }
        } else {
            listOfNotNull(element.owner)
        }
        if (owners.isNotEmpty()) {
            append("A symbol whose [owner] is ")
            if (owners.size == 2) {
                append("either ")
            }
            for ((i, owner) in owners.withIndex()) {
                if (i == owners.lastIndex && i != 0) {
                    append(" or ")
                } else if (i > 0) {
                    append(", ")
                }
                append("[", owner.render(), "]")
            }
            appendLine(".")
            appendLine()
        }
        append(element.extendedKDoc())

        val fieldsWithCurrentSymbol = fieldsWithReferencedSymbols[element].orEmpty()
        if (fieldsWithCurrentSymbol.isNotEmpty()) {
            appendLine()
            for (field in fieldsWithReferencedSymbols[element].orEmpty()) {
                appendLine()
                append("@see ")
                append(field.fieldContainer.render())
                append(".")
                append(field.fieldName)
            }
        }
    }

    override fun filterFields(element: Symbol) =  element.fields.map { field ->
        field.copy().apply {
            if (field in element.parentFields) {
                fromParent = true
            }
        }
    }

    override fun ImportCollectingPrinter.printAdditionalMethods(element: Symbol) {}
}