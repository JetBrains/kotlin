/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.print

import org.jetbrains.kotlin.bir.generator.*
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.Model
import org.jetbrains.kotlin.bir.generator.model.SingleField
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

    print(kind.title, " ", element.typeName)
    print(element.params.typeParameters())
    if (element.kind == ImplementationKind.AbstractClass) {
        print("(elementClass: ${elementClassType.withArgs(TypeRef.Star).render()})")
    }

    val parentRefs = element.parentRefs
    if (parentRefs.isNotEmpty()) {
        print(
            parentRefs.sortedBy { it.typeKind }.joinToString(prefix = " : ") { parent ->
                parent.render() + if ((parent is ElementOrRef<*> && parent.element.typeKind == TypeKind.Class) || parent == elementImplBaseType) {
                    "(elementClass)"
                } else {
                    parent.inheritanceClauseParenthesis()
                }
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
            print("companion object : ${elementClassType.render()}")
            print("<${element.withStarArgs().render()}>(")
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
    .map { element ->
        printGeneratedType(generationPath, TREE_GENERATOR_README, element.packageName, element.typeName) {
            printElement(element)
        }
    }
