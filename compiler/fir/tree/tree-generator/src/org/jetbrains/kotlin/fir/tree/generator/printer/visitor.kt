/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private class VisitorPrinter(
    printer: SmartPrinter,
    visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault) {

    override val visitorType: ClassRef<*> = if (visitSuperTypeByDefault) firDefaultVisitorType else firVisitorType

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>? =
        firVisitorType.takeIf { visitSuperTypeByDefault }?.withArgs(resultTypeVariable, dataTypeVariable)

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun parentInVisitor(element: Element): Element? = when {
        element.isRootElement -> null
        visitSuperTypeByDefault -> element.parentInVisitor
        else -> AbstractFirTreeBuilder.baseFirElement
    }
}

fun printVisitor(elements: List<Element>, generationPath: File, visitSuperTypeByDefault: Boolean) =
    printVisitorCommon(elements, generationPath) { VisitorPrinter(it, visitSuperTypeByDefault) }

private class VisitorVoidPrinter(
    printer: SmartPrinter,
) : AbstractVisitorVoidPrinter<Element, Field>(printer, visitSuperTypeByDefault = false) {

    override val visitorType: ClassRef<*>
        get() = firVisitorVoidType

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
    printVisitorCommon(elements, generationPath, ::VisitorVoidPrinter)

private class DefaultVisitorVoidPrinter(
    printer: SmartPrinter,
) : AbstractVisitorPrinter<Element, Field>(printer, visitSuperTypeByDefault = true) {

    override val visitorType: ClassRef<*>
        get() = firDefaultVisitorVoidType

    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = firVisitorVoidType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun printMethodsForElement(element: Element) {
        printer.run {
            printVisitMethodDeclaration(
                element,
                additionalParameters = emptyList(),
                returnType = StandardTypes.unit,
                modality = Modality.OPEN,
                override = true,
            )
            println(" = ", element.parentInVisitor!!.visitFunctionName, "(", element.visitorParameterName, ")")
            println()
        }
    }
}

fun printDefaultVisitorVoid(elements: List<Element>, generationPath: File) =
    printVisitorCommon(elements, generationPath, ::DefaultVisitorVoidPrinter)

private fun printVisitorCommon(
    elements: List<Element>,
    generationPath: File,
    makePrinter: (SmartPrinter) -> AbstractVisitorPrinter<Element, Field>,
): GeneratedFile {
    val stringBuilder = StringBuilder()
    val smartPrinter = SmartPrinter(stringBuilder)
    val printer = makePrinter(smartPrinter)
    val dir = File(generationPath, printer.visitorType.packageName.replace(".", "/"))
    val file = File(dir, "${printer.visitorType.simpleName}.kt")
    smartPrinter.run {
        printCopyright()
        println("package $VISITOR_PACKAGE")
        println()
        Element.Kind.entries.map { it.fullPackageName }.sorted().forEach {
            println("import ", it, ".*")
        }
        println()
        printGeneratedMessage()
        printer.printVisitor(elements)
    }
    return GeneratedFile(file, stringBuilder.toString())
}