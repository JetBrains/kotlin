/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

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

            referencesData.walkHierarchyTopDown(FIR_ELEMENT_CLASS_NAME) { parent, klass ->
                generateVisit(klass, parent)
            }

            val trampolines = referencesData.back.let {
                it.keys + it.values.flatten()
            }.distinct()
            trampolines.forEach {
                generateTrampolineVisit(it)
            }

        }
        println("}")
    }


    private fun Printer.generateVisit(className: String, parent: String) {
        val shortcutName = className.classNameWithoutFir
        val parameterName = shortcutName.decapitalize().safeName
        generateFunction(
            name = "visit$shortcutName",
            parameters = mapOf(
                parameterName to className
            ),
            returnType = "Unit"
        ) {
            printIndent()
            generateCall("visit${parent.classNameWithoutFir}", listOf(parameterName, "null"))
            println()
        }
    }

    private fun Printer.generateTrampolineVisit(className: String) {
        val shortcutName = className.classNameWithoutFir
        val parameterName = shortcutName.decapitalize().safeName
        generateFunction(
            name = "visit$shortcutName",
            parameters = mapOf(
                parameterName to className,
                "data" to "Nothing?"
            ),
            returnType = "Unit",
            override = true,
            final = true
        ) {
            printIndent()
            generateCall("visit$shortcutName", listOf(parameterName))
            println()
        }
    }
}