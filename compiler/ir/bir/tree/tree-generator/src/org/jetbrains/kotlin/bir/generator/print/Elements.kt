/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.print

import com.intellij.psi.util.PsiExpressionTrimRenderer.render
import org.jetbrains.kotlin.bir.generator.*
import org.jetbrains.kotlin.bir.generator.TREE_GENERATOR_README
import org.jetbrains.kotlin.bir.generator.model.*
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.Model
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val elementAccept = ArbitraryImportable(Packages.tree, "accept")

context(ImportCollector)
private fun SmartPrinter.printElement(element: Element) {
    val kind = element.kind ?: error("Expected non-null element kind")

    printKDoc(element.extendedKDoc("A ${if (element.isLeaf) "leaf" else "non-leaf"} IR tree element."))
    print(kind.title, " ", element.typeName)
    print(element.params.typeParameters())

    val parentRefs = element.parentRefs
    if (parentRefs.isNotEmpty()) {
        print(
            parentRefs.sortedBy { it.typeKind }.joinToString(prefix = " : ") { parent ->
                parent.render() + parent.inheritanceClauseParenthesis()
            }
        )
    }
    print(element.params.multipleUpperBoundsList())

    val body = SmartPrinter(StringBuilder()).apply {
        withIndent {
            for (field in element.fields) {
                printField(
                    field,
                    override = field.isOverride,
                    modality = Modality.ABSTRACT.takeIf { !kind.isInterface },
                )
                println()
            }

            if (element.isLeaf && element.walkableChildren.isNotEmpty()) {
                println()
                val dataTP = TypeVariable("D")
                printFunctionWithBlockBody(
                    name = "acceptChildren",
                    parameters = listOf(
                        FunctionParameter("visitor", elementVisitorType.withArgs(dataTP)),
                        FunctionParameter("data", dataTP)
                    ),
                    returnType = StandardTypes.unit,
                    typeParameters = listOf(dataTP),
                    override = true,
                ) {
                    for (child in element.walkableChildren) {
                        print(child.name)
                        when (child) {
                            is SingleField -> {
                                addImport(elementAccept)
                                if (child.nullable) print("?")
                                println(".accept(data, visitor)")
                            }
                            is ListField -> {
                                println(".acceptChildren(visitor, data)")
                            }
                        }
                    }
                }
            }

            println()
            print("companion object : ${elementClassType.render()}(")
            print("${element.withArgs().render()}::class.java")
            print(", ${element.classId}")
            print(", ${element.isLeaf}")
            println(")")
        }
    }.toString()

    if (body.isNotEmpty()) {
        println(" {")
        print(body.trimStart('\n'))
        print("}")
    }
    println()
}

fun printElements(generationPath: File, model: Model) = model.elements.asSequence()
    .filterNot { it == model.rootElement }
    .map { element ->
        printGeneratedType(generationPath, TREE_GENERATOR_README, element.packageName, element.typeName) {
            printElement(element)
        }
    }
