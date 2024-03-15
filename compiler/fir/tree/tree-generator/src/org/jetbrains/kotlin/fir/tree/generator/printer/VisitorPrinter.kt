/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.baseAbstractElementType
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

internal open class VisitorPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    private val visitSuperTypeByDefault: Boolean,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    lateinit var uselessElementsForDefaultVisiting: Set<Element>

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>? =
        firVisitorType.takeIf { visitSuperTypeByDefault }?.withArgs(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element): TypeRef = resultTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    context(ImportCollector) override fun printVisitor(elements: List<Element>) {
        uselessElementsForDefaultVisiting = getUselessElementsForDefaultVisiting(elements)
        super.printVisitor(elements)
    }

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        if (element == AbstractFirTreeBuilder.baseFirElement) {
            printer.printVisitMethod(AbstractFirTreeBuilder.baseFirAbstractElement, Modality.ABSTRACT, false)
            return
        }

        val isInterface = element.kind?.isInterface == true
        val parentInVisitor = parentInVisitor(element)

        if (isInterface && !visitSuperTypeByDefault) return
        if (!isInterface && visitSuperTypeByDefault && parentInVisitor == AbstractFirTreeBuilder.baseFirAbstractElement) return
        if (visitSuperTypeByDefault && element in uselessElementsForDefaultVisiting) return

        val (modality, override) = when {
            visitSuperTypeByDefault && !isInterface -> null to true
            else -> Modality.OPEN to false
        }

        printer.printVisitMethod(element, modality, override)
    }

    context(ImportCollector)
    protected open fun SmartPrinter.printVisitMethod(element: Element, modality: Modality?, override: Boolean) {
        printMethodDeclarationForElement(element, modality, override)
        val parentInVisitor = parentInVisitor(element)
        if (parentInVisitor != null) {
            println(" =")
            withIndent {
                print(
                    parentInVisitor.visitFunctionName,
                    "(",
                    element.visitorParameterName,
                    element.castToFirElementIfNeeded(),
                    ", data)"
                )
            }
        }
        println()
    }

    override fun skipElement(element: Element): Boolean = visitSuperTypeByDefault && element.isRootElement

    override fun parentInVisitor(element: Element): Element? = when {
        element == AbstractFirTreeBuilder.baseFirAbstractElement -> null
        visitSuperTypeByDefault -> element.parentInVisitor ?: AbstractFirTreeBuilder.baseFirAbstractElement
        else -> AbstractFirTreeBuilder.baseFirAbstractElement
    }

    context (ImportCollector)
    protected fun Element.castToFirElementIfNeeded(): String {
        val isInterface = kind?.isInterface == true
        return if (isInterface && parentInVisitor(element) == AbstractFirTreeBuilder.baseFirAbstractElement) {
            " as ${baseAbstractElementType.render()}"
        } else {
            ""
        }
    }
}

private fun getUselessElementsForDefaultVisiting(elements: List<Element>): Set<Element> {
    val neighbors = DFS.Neighbors<Element> { element ->
        element.parentInVisitor?.let { listOf(it) } ?: emptyList()
    }
    val visited = object: DFS.Visited<Element> {
        val visitedElements = mutableSetOf<Element>()

        override fun checkAndMarkVisited(current: Element): Boolean {
            return visitedElements.add(current)
        }
    }
    val dummyHandler = object : DFS.AbstractNodeHandler<Element, Unit>() {
        override fun result() {}
    }

    val notInterfaceElements = elements.filter { it.kind?.isInterface != true }
    val interfaceElements = elements.filter { it.kind?.isInterface == true }
    DFS.dfs(notInterfaceElements, neighbors, visited, dummyHandler)

    return interfaceElements.toSet() - visited.visitedElements
}
