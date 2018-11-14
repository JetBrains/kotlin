/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.visitors.generator

import org.jetbrains.kotlin.utils.Printer

class ParametricTransformerGenerator(data: DataCollector.ReferencesData) : AbstractVisitorGenerator(data) {
    override fun Printer.generateContent() {
        println("abstract class $PARAMETRIC_TRANSFORMER_NAME<in D> : $SIMPLE_VISITOR_NAME<$TRANSFORMER_RESULT_NAME<$FIR_ELEMENT_CLASS_NAME>, D>() {")
        indented {

            generateFunction(
                "transformElement",
                mapOf(
                    "element" to "E",
                    "data" to "D"
                ),
                constructCompositeBoxType("E"),
                typeParameters = listOf("E : $FIR_ELEMENT_CLASS_NAME"),
                body = null
            )

            referencesData.walkHierarchyTopDown(FIR_ELEMENT_CLASS_NAME) { parent, className ->
                generateTransformMethod(className, parent)
            }

            allElementTypes().forEach {
                generateTrampolineVisit(it)
            }

        }
        println("}")
    }


    fun constructCompositeBoxType(type: String): String {
        return "$TRANSFORMER_RESULT_NAME<$type>"
    }

    val baseTypes = mutableMapOf<String, String>().also {
        for (baseTransformedType in referencesData.baseTransformedTypes) {
            it[baseTransformedType] = baseTransformedType
        }

        data.walkHierarchyTopDown(FIR_ELEMENT_CLASS_NAME) { parent, element ->
            it[element] = it[parent] ?: return@walkHierarchyTopDown
        }
    }

    fun Printer.generateTrampolineVisit(className: String) {
        val shortcutName = className.classNameWithoutFir
        val parameterName = shortcutName.decapitalize().safeName
        generateFunction(
            name = "visit$shortcutName",
            parameters = mapOf(
                parameterName to className,
                "data" to "D"
            ),
            returnType = constructCompositeBoxType(FIR_ELEMENT_CLASS_NAME),
            override = true,
            final = true
        ) {
            print("return ")
            generateCall("transform$shortcutName", listOf(parameterName, "data"))
            println()
        }
    }

    fun Printer.generateTransformMethod(className: String, parent: String) {
        val baseType = baseTypes[className]

        val shortcutName = className.classNameWithoutFir
        val parameterName = shortcutName.decapitalize().safeName
        if (baseType == null) {
            generateFunction(
                "transform$shortcutName",
                mapOf(
                    parameterName to "E",
                    "data" to "D"
                ),
                constructCompositeBoxType("E"),
                typeParameters = listOf("E : $parent")
            ) {
                print("return ")
                generateCall("transform${parent.classNameWithoutFir}", listOf(parameterName, "data"))
                printlnWithNoIndent()
            }
        } else {
            generateFunction(
                "transform$shortcutName",
                mapOf(
                    parameterName to className,
                    "data" to "D"
                ),
                constructCompositeBoxType(baseType)
            ) {
                print("return ")
                generateCall("transform${parent.classNameWithoutFir}", listOf(parameterName, "data"))
                printlnWithNoIndent()
            }
        }

    }

}