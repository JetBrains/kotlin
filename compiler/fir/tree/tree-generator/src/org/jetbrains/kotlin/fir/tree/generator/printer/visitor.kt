/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.firDefaultVisitorType
import org.jetbrains.kotlin.fir.tree.generator.firDefaultVisitorVoidType
import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.firVisitorVoidType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private class VisitorPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>? =
        firVisitorType.takeIf { visitSuperTypeByDefault }?.withArgs(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = resultTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun parentInVisitor(element: Element): Element? = when {
        element.isRootElement -> null
        visitSuperTypeByDefault -> element.parentInVisitor
        else -> AbstractFirTreeBuilder.baseFirElement
    }
}

fun printVisitor(elements: List<Element>, generationPath: File, visitSuperTypeByDefault: Boolean) =
    printVisitorCommon(
        elements,
        generationPath,
        if (visitSuperTypeByDefault) firDefaultVisitorType else firVisitorType,
    ) { printer, visitorType ->
        VisitorPrinter(printer, visitorType, visitSuperTypeByDefault)
    }

private class VisitorVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = firVisitorType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override val useAbstractMethodForRootElement: Boolean
        get() = true

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = true

    override fun parentInVisitor(element: Element): Element = AbstractFirTreeBuilder.baseFirElement
}

fun printVisitorVoid(elements: List<Element>, generationPath: File) =
    printVisitorCommon(elements, generationPath, firVisitorVoidType, ::VisitorVoidPrinter)

private class DefaultVisitorVoidPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = true) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element) = StandardTypes.unit

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = firVisitorVoidType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        printer.run {
            printVisitMethodDeclaration(
                element,
                hasDataParameter = false,
                override = true,
            )
            println(" = ", element.parentInVisitor!!.visitFunctionName, "(", element.visitorParameterName, ")")
            println()
        }
    }
}

fun printDefaultVisitorVoid(elements: List<Element>, generationPath: File) =
    printVisitorCommon(elements, generationPath, firDefaultVisitorVoidType, ::DefaultVisitorVoidPrinter)

private fun printVisitorCommon(
    elements: List<Element>,
    generationPath: File,
    visitorType: ClassRef<*>,
    makePrinter: (SmartPrinter, ClassRef<*>) -> AbstractVisitorPrinter<Element, Field>,
): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, visitorType.packageName, visitorType.simpleName) {
        println()
        makePrinter(this, visitorType).printVisitor(elements)
    }