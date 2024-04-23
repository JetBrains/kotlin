/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollector
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.irImplementationDetailType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Implementation
import org.jetbrains.kotlin.utils.SmartPrinter

internal class ImplementationPrinter(printer: SmartPrinter) : AbstractImplementationPrinter<Implementation, Element, Field>(printer) {
    override fun makeFieldPrinter(printer: SmartPrinter) = object : AbstractFieldPrinter<Field>(printer) {
        override fun forceMutable(field: Field) = field.isMutable
    }

    override val pureAbstractElementType: ClassRef<*>
        get() = org.jetbrains.kotlin.ir.generator.elementBaseType

    override val implementationOptInAnnotation: ClassRef<*>
        get() = irImplementationDetailType

    override val separateFieldsWithBlankLine: Boolean
        get() = true

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods(implementation: Implementation) {
        implementation.generationCallback?.invoke(this@ImportCollector, this)

        if (
            implementation.element.elementAncestorsAndSelfDepthFirst().any { it == IrTree.symbolOwner } &&
            implementation.bindOwnedSymbol
        ) {
            val symbolField = implementation.getOrNull("symbol")
            if (symbolField != null) {
                println()
                print("init")
                printBlock {
                    println("${symbolField.name}.bind(this)")
                }
            }
        }
    }

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalConstructorParameters(implementation: Implementation) {
        if (implementation.element.category == Element.Category.Expression) {
            printlnMultiLine(
                """
                @Suppress("UNUSED_PARAMETER")
                constructorIndicator: ${type(Packages.util, "IrElementConstructorIndicator").render()}?,
                """
            )
        }
    }
}