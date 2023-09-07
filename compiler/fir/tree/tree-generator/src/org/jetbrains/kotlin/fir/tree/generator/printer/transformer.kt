/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.generators.tree.typeWithArguments
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

import java.io.File

fun printTransformer(elements: List<Element>, generationPath: File): GeneratedFile {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    val file = File(dir, "FirTransformer.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class FirTransformer<in D> : FirVisitor<FirElement, D>() {")
        println()
        withIndent {
            println("abstract fun <E : FirElement> transformElement(element: E, data: D): E")
            println()
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                val varName = element.safeDecapitalizedName
                print("open fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                println(
                    "transform${element.name}($varName: ${element.typeWithArguments}, data: D): ${element.transformerType
                        .typeWithArguments}${element.multipleUpperBoundsList()} {",
                )
                withIndent {
                    println("return transformElement($varName, data)")
                }
                println("}")
                println()
            }

            for (element in elements) {
                val varName = element.safeDecapitalizedName
                print("final override fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }

                println(
                    "visit${element.name}($varName: ${element.typeWithArguments}, data: D): ${element.transformerType
                        .typeWithArguments}${element.multipleUpperBoundsList()} {",
                )
                withIndent {
                    println("return transform${element.name}($varName, data)")
                }
                println("}")
                println()
            }
        }
        println("}")
    }
    return GeneratedFile(file, stringBuilder.toString())
}
