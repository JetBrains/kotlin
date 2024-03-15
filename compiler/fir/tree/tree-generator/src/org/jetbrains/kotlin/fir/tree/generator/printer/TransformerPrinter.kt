/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.firVisitorType
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.printBlock
import org.jetbrains.kotlin.utils.SmartPrinter

internal class TransformerPrinter(
    printer: SmartPrinter,
    override val visitorType: ClassRef<*>,
    private val rootElement: Element,
) : AbstractTransformerPrinter<Element, Field>(printer) {

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = firVisitorType.withArgs(AbstractFirTreeBuilder.baseFirAbstractElement, visitorDataType)

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = true

    override fun skipElement(element: Element): Boolean {
        return !element.isRootElement && element.kind?.isInterface == true
    }

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        if (element.isRootElement) {
            super.printMethodsForElement(AbstractFirTreeBuilder.baseFirAbstractElement)
        } else {
            super.printMethodsForElement(element)
        }
    }

    context(ImportCollector) override fun SmartPrinter.printOverriddenVisitMethod(element: Element) {
        printVisitMethodDeclaration(
            element = element,
            modality = Modality.FINAL,
            override = true,
            returnType = AbstractFirTreeBuilder.baseFirAbstractElement
        )
        printBlock {
            val castIfNeeded = buildString {
                if (element.transformerClass.kind?.isInterface == true) {
                    append(" as ${AbstractFirTreeBuilder.baseFirAbstractElement}")
                }
            }
            println(
                "return transform",
                element.name,
                "(",
                element.visitorParameterName,
                ", ",
                "data)",
                castIfNeeded
            )
        }
    }

    override fun parentInVisitor(element: Element) = when {
        element.isRootElement -> null
        else -> AbstractFirTreeBuilder.baseFirAbstractElement
    }
}
