/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.firVisitorVoidType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printBlock

internal class DefaultVisitorVoidPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = firVisitorVoidType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun printMethodsForElement(element: Element) {
        val parentInVisitor = element.parentInVisitor ?: return
        printer.run {
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                override = true,
            )
            printBlock {
                println(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ")")
            }
            println()
        }
    }
}
