/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.irImplementationDetailType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Implementation

internal class ImplementationPrinter(
    printer: ImportCollectingPrinter
) : AbstractImplementationPrinter<Implementation, Element, Field>(printer) {
    override fun makeFieldPrinter(printer: ImportCollectingPrinter) = object : AbstractFieldPrinter<Field>(printer) {
        override fun forceMutable(field: Field) = field.isMutable
    }

    override val pureAbstractElementType: ClassRef<*>
        get() = org.jetbrains.kotlin.ir.generator.elementBaseType

    override val implementationOptInAnnotation: ClassRef<*>
        get() = irImplementationDetailType

    override val separateFieldsWithBlankLine: Boolean
        get() = true

    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
        implementation.generationCallback?.invoke(this)

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

    override fun ImportCollectingPrinter.printAdditionalConstructorParameters(implementation: Implementation) {
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