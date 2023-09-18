/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.multipleUpperBoundsList
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.generators.tree.printer.typeParameters
import org.jetbrains.kotlin.generators.tree.typeWithArguments
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val elementsWithMultipleSupertypesForDefaultVisitor = mapOf(
    FirTreeBuilder.resolvedErrorReference to FirTreeBuilder.resolvedNamedReference
)

private fun Element.isAcceptableForDefaultVisiting(): Boolean {
    if (this == AbstractFirTreeBuilder.baseFirElement) return false
    val hasSingleSupertype = elementParents.size == 1 && !elementParents[0].element.isRootElement
    return hasSingleSupertype || this in elementsWithMultipleSupertypesForDefaultVisitor
}

private fun Element.getNameOfSupertypeForDefaultVisiting(): String {
    val parentForDefaultVisiting =
        elementParents.singleOrNull()?.element ?: elementsWithMultipleSupertypesForDefaultVisitor.getValue(this)
    return parentForDefaultVisiting.name
}

fun printVisitor(elements: List<Element>, generationPath: File, visitSuperTypeByDefault: Boolean): GeneratedFile {
    val className = if (visitSuperTypeByDefault) "FirDefaultVisitor" else "FirVisitor"
    return printGeneratedType(generationPath, TREE_GENERATOR_README, VISITOR_PACKAGE, className) {
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()

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
            if (element == AbstractFirTreeBuilder.baseFirElement) continue
            if (visitSuperTypeByDefault && !element.isAcceptableForDefaultVisiting()) continue
            with(element) {
                val varName = safeDecapitalizedName
                if (visitSuperTypeByDefault) {
                    print("override")
                } else {
                    print("open")
                }
                print(" fun ${typeParameters(end = " ")}visit$name($varName: $typeWithArguments, data: D): R${multipleUpperBoundsList()} = visit")
                if (visitSuperTypeByDefault) {
                    print(element.getNameOfSupertypeForDefaultVisiting())
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
}


fun printVisitorVoid(elements: List<Element>, generationPath: File): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, VISITOR_PACKAGE, "FirVisitorVoid") {
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        println("abstract class FirVisitorVoid : FirVisitor<Unit, Nothing?>() {")

        withIndent {
            println("abstract fun visitElement(element: FirElement)")
            println()
            for (element in elements) {
                if (element == AbstractFirTreeBuilder.baseFirElement) continue
                with(element) {
                    val varName = safeDecapitalizedName
                    println("open fun ${typeParameters(end = " ")}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()} {")
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
                    println("final override fun ${typeParameters(end = " ")}visit$name($varName: $typeWithArguments, data: Nothing?)${multipleUpperBoundsList()} {")
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

fun printDefaultVisitorVoid(elements: List<Element>, generationPath: File): GeneratedFile =
    printGeneratedType(generationPath, TREE_GENERATOR_README, VISITOR_PACKAGE, "FirDefaultVisitorVoid") {
        elements.forEach { println("import ${it.fullQualifiedName}") }
        println()
        println("abstract class FirDefaultVisitorVoid : FirVisitorVoid() {")

        pushIndent()
        for (element in elements) {
            if (!element.isAcceptableForDefaultVisiting()) continue
            with(element) {
                val varName = safeDecapitalizedName
                println("override fun ${typeParameters(end = " ")}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()} = visit${element.getNameOfSupertypeForDefaultVisiting()}($varName)")
                println()
            }
        }
        popIndent()
        println("}")
    }
