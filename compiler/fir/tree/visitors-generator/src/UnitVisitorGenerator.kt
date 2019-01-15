/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import org.jetbrains.kotlin.fir.visitors.generator.DataCollector.NameWithTypeParameters
import org.jetbrains.kotlin.utils.Printer

class UnitVisitorGenerator(referencesData: DataCollector.ReferencesData) : AbstractVisitorGenerator(referencesData) {
    override fun Printer.generateContent() {
        println("abstract class $UNIT_VISITOR_NAME : $SIMPLE_VISITOR_NAME<Unit, Nothing?>() {")
        indented {
            generateFunction(
                "visitElement",
                parameters = mapOf(
                    "element" to FIR_ELEMENT_CLASS_NAME
                ),
                returnType = "Unit",
                body = null
            )

            referencesData.walkHierarchyTopDown(from = FIR_ELEMENT_CLASS_NAME) { parent, element ->
                generateVisit(element, parent)
            }

            allElementTypes().forEach {
                generateTrampolineVisit(it)
            }

        }
        println("}")
    }


    private fun Printer.generateVisit(
        className: NameWithTypeParameters,
        parent: NameWithTypeParameters
    ) {
        val shortcutName = className.name.classNameWithoutFir
        val parameterName = shortcutName.decapitalize().safeName
        generateFunction(
            name = "visit$shortcutName",
            parameters = mapOf(
                parameterName to className
            ),
            returnType = "Unit",
            typeParametersWithBounds = className.typeParametersWithBounds()
        ) {
            printIndent()
            generateCall("visit${parent.name.classNameWithoutFir}", listOf(parameterName, "null"))
            println()
        }
    }

    private fun Printer.generateTrampolineVisit(className: NameWithTypeParameters) {
        val shortcutName = className.name.classNameWithoutFir
        val parameterName = shortcutName.decapitalize().safeName
        generateFunction(
            name = "visit$shortcutName",
            parameters = mapOf(
                parameterName to className,
                "data" to NameWithTypeParameters("Nothing?")
            ),
            returnType = "Unit",
            override = true,
            final = true,
            typeParametersWithBounds = className.typeParametersWithBounds()
        ) {
            printIndent()
            generateCall("visit$shortcutName", listOf(parameterName))
            println()
        }
    }
}