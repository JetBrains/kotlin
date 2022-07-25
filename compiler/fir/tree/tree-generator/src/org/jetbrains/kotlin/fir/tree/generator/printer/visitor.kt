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
import org.jetbrains.kotlin.fir.tree.generator.model.Field
import org.jetbrains.kotlin.fir.tree.generator.model.FieldList
import org.jetbrains.kotlin.fir.tree.generator.model.FirField
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

fun printVisitor(elements: List<Element>, generationPath: File, visitSuperTypeByDefault: Boolean): GeneratedFile {
    val className = if (visitSuperTypeByDefault) "FirDefaultVisitor" else "FirVisitor"
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    val file = File(dir, "$className.kt")
    val stringBuilder = StringBuilder()

    val elementsWithChildren = mutableSetOf<Element>()
    for (element in elements) {
        element.parents.forEach { elementsWithChildren.add(it) }
    }

    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("@file:Suppress(\"UNUSED_PARAMETER\")")
        println("package $VISITOR_PACKAGE")
        println()
        println("import $VISITOR_PACKAGE.FirElementKind.*")
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        print("abstract class $className<out R, in D> ")
        if (visitSuperTypeByDefault) {
            print(": FirVisitor<R, D>() ")
        }
        println("{")

        pushIndent()
        if (!visitSuperTypeByDefault) {
            println("abstract fun visitElement(element: FirElement, data: D): R\n")
            println()
            println("fun dispatch(element: FirElement, data: D): R {")
            pushIndent()
            println("return when (element.elementKind) {")
            pushIndent()
            for (element in elements) {
                if (element.allImplementations.isNotEmpty() && element in elementsWithChildren) {
                    println("// ${element.name} -> ${element.allImplementations.map { it.name }}")
                }
                if (element in elementsWithChildren && element.allImplementations.isEmpty() && !element.hasManualImplementations) continue
                val typeArgumentsForCast = if (element.typeArguments.isNotEmpty()) {
                    element.typeArguments.joinToString(separator = ",", prefix = "<", postfix = ">") { "*" }
                } else ""
                println("${element.name} -> visit${element.name}(element as ${element.type}$typeArgumentsForCast, data)")
            }
            popIndent()
            println("}")
            popIndent()
            println("}")
            println()
            println("fun dispatchChildren(element: FirElement, data: D) {")
            pushIndent()
            println("when (element.elementKind) {")
            pushIndent()
            for (element in elements) {
                if (element.allImplementations.isNotEmpty() && element in elementsWithChildren) {
                    println("// ${element.name} -> ${element.allImplementations.map { it.name }}")
                }
                if (element in elementsWithChildren && element.allImplementations.isEmpty() && !element.hasManualImplementations) continue
                println("${element.name} -> visit${element.name}Children(element as ${element.type}${element.erasedTypeArguments}, data)")
            }
            popIndent()
            println("}")
            popIndent()
            println("}")
        }
        for (element in elements) {
            if (element == AbstractFirTreeBuilder.baseFirElement ||
                visitSuperTypeByDefault && (element.parents.size != 1 || element.parents.single().name == "Element")
            ) continue
            with(element) {
                val varName = safeDecapitalizedName
                if (visitSuperTypeByDefault) {
                    print("override")
                } else {
                    print("open")
                }
                print(" fun ${typeParameters}visit$name($varName: $typeWithArguments, data: D): R${multipleUpperBoundsList()} = visit")
                if (visitSuperTypeByDefault) {
                    print(parents.single().name)
                } else {
                    print("Element")
                }
                println("($varName, data)")
                println()

                if (!visitSuperTypeByDefault) {
                    println("private fun ${typeParameters}visit${name}Children($varName: $typeWithArguments, data: D) {")

                    fun Field.acceptString(): String = "$varName.${name}${call()}accept(this, data)"
//                        if (!isInterface && !isAbstract) {
//                            print("override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {")

                    if (element.allFirFields.isNotEmpty()) {
                        withIndent {
                            for (field in allFirFields) {
                                if (field.withGetter || field.nonReplaceable ?: false) continue
                                if (field is FirField && field.nonTraversable) continue
                                if (element == FirTreeBuilder.userTypeRef) {
                                    // TODO
                                    println(
                                        """
                                        |for (part in $varName.qualifier) {
                                        |    part.typeArgumentList.typeArguments.forEach { it.accept(this, data) }
                                        |}
                                        """.trimMargin()
                                    )
                                }
                                when (field.name) {
                                    "dispatchReceiver", "extensionReceiver", "subjectVariable", "companionObject" -> {
                                    }

                                    "explicitReceiver" -> {
                                        val explicitReceiver = element["explicitReceiver"]!!
                                        val dispatchReceiver = element["dispatchReceiver"]!!
                                        val extensionReceiver = element["extensionReceiver"]!!
                                        println(
                                            """
                                    |${explicitReceiver.acceptString()}
                                    |        if ($varName.dispatchReceiver !== $varName.explicitReceiver) {
                                    |            ${dispatchReceiver.acceptString()}
                                    |        }
                                    |        if ($varName.extensionReceiver !== $varName.explicitReceiver && $varName.extensionReceiver !== $varName.dispatchReceiver) {
                                    |            ${extensionReceiver.acceptString()}
                                    |        }
                                        """.trimMargin(),
                                        )
                                    }

                                    else -> {
                                        if (type == "FirWhenExpression" && field.name == "subject") {
                                            println(
                                                """
                                        |val subjectVariable_ = $varName.subjectVariable
                                        |        if (subjectVariable_ != null) {
                                        |            subjectVariable_.accept(this, data)
                                        |        } else {
                                        |            $varName.subject?.accept(this, data)
                                        |        }
                                            """.trimMargin(),
                                            )
                                        } else {
                                            when (field) {
                                                is FirField -> {
                                                    println(field.acceptString())
                                                }

                                                is FieldList -> {
                                                    println("$varName.${field.name}.forEach { it.accept(this, data) }")
                                                }

                                                else -> throw IllegalStateException()
                                            }
                                        }
                                    }

                                }
                            }
                        }
                    }
                    println("}")
                    println()
                }

            }
        }
        popIndent()
        println("}")
    }
    return GeneratedFile(file, stringBuilder.toString())
}


fun printVisitorVoid(elements: List<Element>, generationPath: File): GeneratedFile {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    val file = File(dir, "FirVisitorVoid.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class FirVisitorVoid : FirVisitor<Unit, Nothing?>() {")

        withIndent {
            println("abstract fun visitElement(element: FirElement)")
            println()
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                with(element) {
                    val varName = safeDecapitalizedName
                    println("open fun ${typeParameters}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()}{")
                    withIndent {
                        println("visitElement($varName)")
                    }
                    println("}")
                    println()
                }
            }

            for (element in elements) {
                with(element) {
                    val varName = safeDecapitalizedName
                    println("final override fun ${typeParameters}visit$name($varName: $typeWithArguments, data: Nothing?)${multipleUpperBoundsList()}{")
                    withIndent {
                        println("visit$name($varName)")
                    }
                    println("}")
                    println()
                }
            }
        }
        println("}")
    }
    return GeneratedFile(file, stringBuilder.toString())
}

fun printDefaultVisitorVoid(elements: List<Element>, generationPath: File): GeneratedFile {
    val className = "FirDefaultVisitorVoid"
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    val file = File(dir, "$className.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class $className : FirVisitorVoid() {")

        pushIndent()
        for (element in elements) {
            if (element == AbstractFirTreeBuilder.baseFirElement || element.parents.size != 1 || element.parents.single().name == "Element") continue
            with(element) {
                val varName = safeDecapitalizedName
                println("override fun ${typeParameters}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()} = visit${parents.first().name}($varName)")
                println()
            }
        }
        popIndent()
        println("}")
    }
    return GeneratedFile(file, stringBuilder.toString())
}
