/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.irVisitorVoidType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.symbol.symbolVisitorMethodName
import org.jetbrains.kotlin.ir.generator.symbolVisitorType

internal class IrTreeSymbolsVisitorPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    rootElement: Element
) : TypeVisitorVoidPrinter(printer, visitorType, rootElement) {
    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irVisitorVoidType, symbolVisitorType)

    override val implementationKind: ImplementationKind
        get() = ImplementationKind.AbstractClass

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        println()
        printVisitTypeMethod(name = "visitType", hasDataParameter = false, modality = Modality.OPEN, override = false)
        println(" {}")
        println()
        printVisitTypeRecursively(hasDataParameter = false)
    }

    override fun shouldPrintVisitWithDataMethod(): Boolean = false

    override fun printVisitVoidMethodDeclaration(element: Element) = with(printer) {
        printVisitMethodDeclaration(element, hasDataParameter = false, modality = null, override = true)
    }

    override fun printTypeRemappingsOverridable(
        printer: ImportCollectingPrinter,
        element: Element,
        irTypeFields: List<Field>,
        hasDataParameter: Boolean,
        transformTypes: Boolean,
    ): Unit = with(printer) {
        if (element.implementations.isNotEmpty()) {
            val fieldsWithSymbol = element.fields.filter { it.symbolClass != null }
            fieldsWithSymbol.forEach { visitField(element, it) }
        }
        super.printTypeRemappingsOverridable(printer, element, irTypeFields, hasDataParameter, transformTypes)
    }

    private fun ImportCollectingPrinter.visitField(element: Element, field: Field) {
        if (field is ListField) {
            val safeCall = if (field.typeRef.nullable) "?." else "."
            print(element.visitorParameterName, ".", field.name, safeCall, "forEach { ")
            visitSymbolValue(field, "it")
            print(" }")
        } else {
            visitSymbolValue(field, element.visitorParameterName, ".", field.name)
        }
        println()
    }

    private fun ImportCollectingPrinter.visitSymbolValue(field: Field, vararg valueArgs: Any?) {
        val symbolFieldClass = field.symbolClass!!
        val typeRef = if (field is ListField) {
            field.baseType
        } else {
            field.typeRef
        } as? ClassOrElementRef ?: return

        val symbolVisitFunction =
            symbolVisitorMethodName(symbolFieldClass, field.symbolFieldRole ?: AbstractField.SymbolFieldRole.REFERENCED)
        if (typeRef.nullable) {
            print(*valueArgs, "?.let(::", symbolVisitFunction, ")")
        } else {
            print(symbolVisitFunction, "(", *valueArgs, ")")
        }
    }
}
