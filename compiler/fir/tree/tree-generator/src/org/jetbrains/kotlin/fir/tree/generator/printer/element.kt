/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.util.get
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private class ElementPrinter(printer: SmartPrinter) : AbstractElementPrinter<Element, Field>(printer) {

    override fun makeFieldPrinter(printer: SmartPrinter) = object : AbstractFieldPrinter<Field>(printer) {}

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods(element: Element) {
        val kind = element.kind ?: error("Expected non-null element kind")
        with(element) {
            printAcceptMethod(element, firVisitorType, hasImplementation = true, treeName = "FIR")

            printTransformMethod(
                element = element,
                transformerClass = firTransformerType,
                implementation = "transformer.transform${element.name}(this, data)",
                returnType = TypeVariable("E", listOf(AbstractFirTreeBuilder.baseFirElement)),
                treeName = "FIR",
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
                val override = overridenFields[it, it] && !(it.name == "source" && element == FirTreeBuilder.qualifiedAccessExpression)
                it.replaceDeclaration(override, forceNullable = it.useNullableForReplace)
                for (overriddenType in it.overridenTypes) {
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
                println("fun accept(visitor: ", firVisitorVoidType.render(), ") = accept(visitor, null)")

                printAcceptChildrenMethod(
                    element = element,
                    visitorClass = firVisitorType,
                    visitorResultType = TypeVariable("R"),
                )
                println()
                println()
                println("fun acceptChildren(visitor: ", firVisitorVoidType.render(), ") = acceptChildren(visitor, null)")

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

fun Element.generateCode(generationPath: File): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, packageName, typeName) {
        ElementPrinter(this).printElement(element)
    }
