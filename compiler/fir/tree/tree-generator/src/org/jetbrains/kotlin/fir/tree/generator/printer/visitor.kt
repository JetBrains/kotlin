/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.FirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.baseAbstractElementType
import org.jetbrains.kotlin.fir.tree.generator.context.AbstractFirTreeBuilder
import org.jetbrains.kotlin.fir.tree.generator.model.Element
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent
import java.io.File

val additionalElements = listOf(
    "StubStatement" to "Statement",
    "DeclarationStatusImpl" to "DeclarationStatus"
)

val additionalImports = listOf(
    "org.jetbrains.kotlin.fir.expressions.impl.FirStubStatement",
    "org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl",
    "org.jetbrains.kotlin.fir.${baseAbstractElementType.type}"
)

fun SmartPrinter.printAdditionalImportForVisitor() {
    additionalImports.forEach { println("import $it") }
}

private val elementsWithMultipleSupertypesForDefaultVisitor = mapOf(
    FirTreeBuilder.resolvedErrorReference to FirTreeBuilder.resolvedNamedReference
)

private fun Element.isAcceptableForDefaultVisiting(): Boolean {
    if (this == AbstractFirTreeBuilder.baseFirElement) return false
    val hasSingleSupertype = parents.size == 1 && (parents.single().name != AbstractFirTreeBuilder.baseFirElement.name || kind?.isInterface == true)
    return hasSingleSupertype || this in elementsWithMultipleSupertypesForDefaultVisitor
}

private fun Element.getSupertypeForDefaultVisiting(): Element {
    return parents.singleOrNull() ?: elementsWithMultipleSupertypesForDefaultVisitor.getValue(this)
}

private fun Element.getNameOfSupertypeForDefaultVisiting(): String {
    val parentForDefaultVisiting = parents.singleOrNull() ?: elementsWithMultipleSupertypesForDefaultVisitor.getValue(this)
    return if (parentForDefaultVisiting == AbstractFirTreeBuilder.baseFirElement) {
        "Element"
    } else {
        parentForDefaultVisiting.name
    }
}

fun Element.castToFirElementIfNeeded(): String {
    val isInterface = kind?.isInterface == true
    return if (isInterface && getSupertypeForDefaultVisiting() == AbstractFirTreeBuilder.baseFirElement) {
        " as ${baseAbstractElementType.type}"
    } else {
        ""
    }
}

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
        printAdditionalImportForVisitor()
        println()
        printGeneratedMessage()

        print("abstract class $className<out R, in D> ")
        if (visitSuperTypeByDefault) {
            print(": FirVisitor<R, D>() ")
        }
        println("{")

        pushIndent()
        if (!visitSuperTypeByDefault) {
            println("abstract fun visitElement(element: ${baseAbstractElementType.type}, data: D): R\n")
        }
        for (element in elements) {
            val isInterface = element.kind?.isInterface == true

            if (element == AbstractFirTreeBuilder.baseFirElement) continue
            if (visitSuperTypeByDefault && !element.isAcceptableForDefaultVisiting()) continue

            // Visiting interfaces is only available in default visitors (as fir interfaces don't have `accept` methods)
            if (!visitSuperTypeByDefault && isInterface) continue

            with(element) {
                val varName = safeDecapitalizedName
                // If element is interface => introduce new method, otherwise override it from FirVisitor
                if (visitSuperTypeByDefault && !isInterface) {
                    print("override")
                } else {
                    print("open")
                }
                print(" fun ${typeParameters}visit$name($varName: $typeWithArguments, data: D): R${multipleUpperBoundsList()} = visit")
                if (visitSuperTypeByDefault) {
                    print(element.getNameOfSupertypeForDefaultVisiting())
                } else {
                    print("Element")
                }

                println("($varName${castToFirElementIfNeeded()}, data)")
                println()
            }
        }

        for ((element, supertype) in additionalElements) {
            val varName = element.replaceFirstChar(Char::lowercaseChar)
            if (visitSuperTypeByDefault) {
                print("override fun visit$element($varName: Fir$element, data: D): R = visit$supertype($varName, data)")
            } else {
                print("open fun visit$element($varName: Fir$element, data: D): R = visitElement($varName, data)")
            }
            println()
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
        printAdditionalImportForVisitor()
        println()
        printGeneratedMessage()

        println("abstract class FirVisitorVoid : FirVisitor<Unit, Nothing?>() {")

        withIndent {
            println("abstract fun visitElement(element: ${baseAbstractElementType.type})")
            println()
            val notInterfaceElements = elements.filter { it.kind?.isInterface == false }

            for (element in notInterfaceElements) {
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

            for ((element, _) in additionalElements) {
                val varName = element.replaceFirstChar(Char::lowercaseChar)
                println("open fun visit$element($varName: Fir$element) {")
                withIndent {
                    println("visitElement($varName)")
                }
                println("}")
                println()
            }

            println("final override fun visitElement(element: ${baseAbstractElementType.type}, data: Nothing?) {")
            withIndent { println("visitElement(element)") }
            println("}")
            println()

            for (element in notInterfaceElements) {
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

            for ((element, _) in additionalElements) {
                val varName = element.replaceFirstChar(Char::lowercaseChar)
                println("final override fun visit$element($varName: Fir$element, data: Nothing?) {")
                withIndent {
                    println("visit$element($varName)")
                }
                println("}")
                println()
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
        printAdditionalImportForVisitor()
        println()
        printGeneratedMessage()

        println("abstract class $className : FirVisitorVoid() {")

        pushIndent()
        for (element in elements) {
            val isInterface = element.kind?.isInterface == true

            if (!element.isAcceptableForDefaultVisiting()) continue
            if (isInterface) {
                print("open")
            } else {
                print("override")
            }

            with(element) {
                val varName = safeDecapitalizedName
                print(" fun ${typeParameters}visit$name($varName: $typeWithArguments)${multipleUpperBoundsList()} = visit")

                print(element.getNameOfSupertypeForDefaultVisiting())
                println("($varName${castToFirElementIfNeeded()})")

                println()
            }
        }

        for ((element, supertype) in additionalElements) {
            val varName = element.replaceFirstChar(Char::lowercaseChar)
            println("override fun visit$element($varName: Fir$element) = visit$supertype($varName)")
            println()
        }
        popIndent()
        println("}")
    }
    return GeneratedFile(file, stringBuilder.toString())
}
