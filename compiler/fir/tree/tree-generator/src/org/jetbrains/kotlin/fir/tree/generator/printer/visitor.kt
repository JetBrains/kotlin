/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element

import java.io.File
import java.io.PrintWriter

fun printVisitor(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "FirVisitor.kt").useSmartPrinter {
        printCopyright()
        println("package $VISITOR_PACKAGE")
        println()
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        printGeneratedMessage()

        println("abstract class FirVisitor<out R, in D> {")

        pushIndent()
        println("abstract fun visitElement(element: FirElement, data: D): R\n")
        for (element in elements) {
            if (element == AbstractFirTreeBuilder.baseFirElement) continue
            with(element) {
                val varName = safeDecapitalizedName
                println("open fun ${typeParameters}visit$name($varName: $typeWithArguments, data: D): R${multipleUpperBoundsList()} = visitElement($varName, data)")
                println()
            }
        }
        popIndent()
        println("}")
    }
}


fun printVisitorVoid(elements: List<Element>, generationPath: File) {
    val dir = File(generationPath, VISITOR_PACKAGE.replace(".", "/"))
    dir.mkdirs()
    File(dir, "FirVisitorVoid.kt").useSmartPrinter {
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
}
