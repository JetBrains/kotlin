/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter

internal class ElementPrinter(printer: SmartPrinter) : AbstractElementPrinter<Element, Field>(printer) {

    override fun makeFieldPrinter(printer: SmartPrinter) = object : AbstractFieldPrinter<Field>(printer) {}

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods(element: Element) {
        val kind = element.kind ?: error("Expected non-null element kind")
        with(element) {
            val treeName = "FIR"
            printAcceptMethod(element, firVisitorType, hasImplementation = true, treeName = treeName)

            printTransformMethod(
                element = element,
                transformerClass = firTransformerType,
                implementation = "transformer.transform${element.name}(this, data)",
                returnType = TypeVariable("E", listOf(AbstractFirTreeBuilder.baseFirElement)),
                treeName = treeName,
            )

            fun Field.replaceDeclaration(override: Boolean, overridenType: TypeRefWithNullability? = null, forceNullable: Boolean = false) {
                println()
                if (name == "source") {
                    println("@", firImplementationDetailType.render())
                }
                replaceFunctionDeclaration(this, override, kind, overridenType, forceNullable)
                println()
            }

            allFields.filter { it.withReplace }.forEach {
                val override = overriddenFieldsHaveSameClass[it, it] && !(it.name == "source" && element == FirTreeBuilder.qualifiedAccessExpression)
                it.replaceDeclaration(override, forceNullable = it.useNullableForReplace)
                for (overriddenType in it.overriddenTypes) {
                    it.replaceDeclaration(true, overriddenType)
                }
            }

            for (field in allFields) {
                if (!field.needsSeparateTransform) continue
                println()
                transformFunctionDeclaration(field, element, override = field.fromParent && field.parentHasSeparateTransform, kind)
                println()
            }
            if (needTransformOtherChildren) {
                println()
                transformOtherChildrenFunctionDeclaration(
                    element,
                    override = element.elementParents.any { it.element.needTransformOtherChildren },
                    kind,
                )
                println()
            }

            if (element.isRootElement) {
                println()
                printAcceptVoidMethod(firVisitorVoidType, treeName)
                printAcceptChildrenMethod(
                    element = element,
                    visitorClass = firVisitorType,
                    visitorResultType = TypeVariable("R"),
                )
                println()
                println()
                printAcceptChildrenVoidMethod(firVisitorVoidType)
                printTransformChildrenMethod(
                    element = element,
                    transformerClass = firTransformerType,
                    returnType = AbstractFirTreeBuilder.baseFirElement,
                )
                println()
            }
        }
    }
}
