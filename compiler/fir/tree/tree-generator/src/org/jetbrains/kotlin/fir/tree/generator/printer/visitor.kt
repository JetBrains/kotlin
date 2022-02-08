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

fun printVisitor(elements: List<Element>, generationPath: File, visitSuperTypeByDefault: Boolean): GeneratedFile {
    val className = if (visitSuperTypeByDefault) "FirDefaultVisitor" else "FirVisitor"
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

        print("abstract class $className<out R, in D> ")
        if (visitSuperTypeByDefault) {
            print(": FirVisitor<R, D>() ")
        }
        println("{")

        pushIndent()
        if (!visitSuperTypeByDefault) {
            println("abstract fun visitElement(element: FirElement, data: D): R\n")
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
