/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*

private val elementsWithReplaceSource = setOf(
    FirTreeBuilder.qualifiedAccessExpression,
    FirTreeBuilder.delegatedConstructorCall,
)

internal class ElementPrinter(printer: ImportCollectingPrinter) : AbstractElementPrinter<Element, Field>(printer) {

    override fun makeFieldPrinter(printer: ImportCollectingPrinter) = object : AbstractFieldPrinter<Field>(printer) {}

    override fun ImportCollectingPrinter.printAdditionalMethods(element: Element) {
        val kind = element.kind ?: error("Expected non-null element kind")
        with(element) {
            val treeName = "FIR"
            printAcceptMethod(element, firVisitorType, hasImplementation = true, treeName = treeName)

            printTransformMethod(
                element = element,
                transformerClass = firTransformerType,
                implementation = "transformer.transform${element.name}(this, data)",
                returnType = TypeVariable("E", listOf(FirTreeBuilder.baseFirElement)),
                treeName = treeName,
            )

            fun Field.replaceDeclaration(
                override: Boolean,
                overriddenType: TypeRefWithNullability? = null,
                forceNullable: Boolean = false,
            ) {
                println()
                if (name == "source") {
                    println("@", firImplementationDetailType.render())
                }
                replaceFunctionDeclaration(this, override, kind, overriddenType, forceNullable)
                println()
            }

            allFields.filter { it.withReplace }.forEach { field ->
                val clazz = field.typeRef.copy(nullable = false)
                val overriddenClasses = field.overriddenFields.map { it -> it.typeRef.copy(nullable = false) }.toSet()

                val override = clazz in overriddenClasses && !(field.name == "source" && element in elementsWithReplaceSource)
                field.replaceDeclaration(override, forceNullable = field.receiveNullableTypeInReplace)

                for (overriddenClass in overriddenClasses - clazz) {
                    field.replaceDeclaration(true, overriddenType = overriddenClass)
                }
            }

            for (field in allFields) {
                if (!field.needsSeparateTransform) continue
                println()
                transformFunctionDeclaration(
                    field = field,
                    returnType = element.withSelfArgs(),
                    override = field.overriddenFields.any { it.needsSeparateTransform },
                    implementationKind = kind
                )
                println()
            }
            if (needTransformOtherChildren) {
                println()
                transformOtherChildrenFunctionDeclaration(
                    element.withSelfArgs(),
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
                    returnType = FirTreeBuilder.baseFirElement,
                )
                println()
            }
        }
    }
}
