/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent

import java.io.File
import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.FirField

fun printTransformer(elements: List<Element>, generationPath: File): GeneratedFile {

    val elementsWithChildren = mutableSetOf<Element>()
    for (element in elements) {
        element.parents.forEach { elementsWithChildren.add(it) }
    }

    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    val file = File(dir, "FirTransformer.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("@file:Suppress(\"UNUSED_PARAMETER\")")
        println("package $VISITOR_PACKAGE")
        println()
        println("import $VISITOR_PACKAGE.FirElementKind.*")
        elements.forEach { println("import ${it.fullQualifiedName}") }
        elements.flatMap {
            it.allFirFields.map { field -> "${it.packageName}.transform${field.name.replaceFirstChar(Char::uppercaseChar)}" }
        }.distinct().forEach {
            print("import ")
            println(it)
        }
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
                    "transform${element.name}($varName: ${element.typeWithArguments}, data: D): ${element.transformerType.typeWithArguments}${element.multipleUpperBoundsList()}{",
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
                    "visit${element.name}($varName: ${element.typeWithArguments}, data: D): ${element.transformerType.typeWithArguments}${element.multipleUpperBoundsList()}{",
                )
                withIndent {
                    println("return transform${element.name}($varName, data)")
                }
                println("}")
                println()
            }

            for (element in elements) {
                val varName = element.safeDecapitalizedName
                print("fun ")
                element.typeParameters.takeIf { it.isNotBlank() }?.let { print(it) }
                print("transform${element.name}Children($varName: ${element.typeWithArguments}, data: D): ${element.transformerType.typeWithArguments}${element.multipleUpperBoundsList()}")
                //if (!isInterface && !isAbstract) {
                println("{")
                withIndent {
                    if (element == FirTreeBuilder.userTypeRef) {
                        // TODO
                        println(
                            """
                            |for (part in $varName.qualifier) {
                            |    (part.typeArgumentList.typeArguments as MutableList<FirTypeProjection>).transformInplace(this, data)
                            |}
                            """.trimMargin()
                        )
                    }
                    for (field in element.allFirFields) {
                        if (field.nonReplaceable ?: false) continue
                        if (field is FirField && field.nonTraversable) continue
                        when {
                            field.name in setOf("dispatchReceiver", "extensionReceiver", "subjectVariable", "companionObject") -> {}
                            field.name == "explicitReceiver" -> {
                                println("$varName.transformExplicitReceiver(this, data)")
                                println(
                                    """
                                |if ($varName.dispatchReceiver !== $varName.explicitReceiver) {
                                |              $varName.transformDispatchReceiver(this, data)
                                |        }
                                """.trimMargin(),
                                )
                                println(
                                    """
                                    |if ($varName.extensionReceiver !== $varName.explicitReceiver && $varName.extensionReceiver !== $varName.dispatchReceiver) {
                                    |            $varName.transformExtensionReceiver(this, data)
                                    |        }
                                    """.trimMargin(),
                                )
                            }

                            else -> {
                                println("${varName}.transform${field.name.replaceFirstChar(Char::uppercaseChar)}(this, data)")
                            }
                        }


                    }
                    println("return $varName")
                }
                println("}")
                println()
            }


            println("fun dispatchTransformChildren(element: FirElement, data: D): FirElement {")
            withIndent {
                println("return when (element.elementKind) {")
                withIndent {
                    for (element in elements) {
                        if (element in elementsWithChildren && element.allImplementations.isEmpty() && !element.hasManualImplementations) continue
                        println("${element.name} -> transform${element.name}Children(element as ${element.type}${element.erasedTypeArguments}, data)")
                    }
                }
                println("}")
            }
            println("}")

        }
        println("}")
    }


    return GeneratedFile(file, stringBuilder.toString())
}