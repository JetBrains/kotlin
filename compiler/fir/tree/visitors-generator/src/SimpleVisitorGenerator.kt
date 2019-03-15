/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import org.jetbrains.kotlin.fir.visitors.generator.DataCollector.NameWithTypeParameters
import org.jetbrains.kotlin.utils.Printer

class SimpleVisitorGenerator(referencesData: DataCollector.ReferencesData) : AbstractVisitorGenerator(referencesData) {
    override fun Printer.generateContent() {
        println("abstract class $SIMPLE_VISITOR_NAME<out R, in D> {")
        indented {
            generateFunction(
                "visitElement",
                parameters = mapOf(
                    "element" to FIR_ELEMENT_CLASS_NAME,
                    "data" to NameWithTypeParameters("D")
                ),
                returnType = "R",
                body = null
            )

            referencesData.walkHierarchyTopDown(from = FIR_ELEMENT_CLASS_NAME) { parent, element ->
                generateVisit(element, parent)
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
                parameterName to className,
                "data" to NameWithTypeParameters("D")
            ),
            returnType = "R",
            typeParametersWithBounds = className.typeParametersWithBounds()
        ) {
            print("return ")
            generateCall("visit${parent.name.classNameWithoutFir}", listOf(parameterName, "data"))
            println()
        }
    }
}