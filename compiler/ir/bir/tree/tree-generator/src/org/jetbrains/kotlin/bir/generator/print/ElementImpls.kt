/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.bir.generator.print

import org.jetbrains.kotlin.bir.generator.BirTree.rootElement
import org.jetbrains.kotlin.bir.generator.Packages
import org.jetbrains.kotlin.bir.generator.TREE_GENERATOR_README
import org.jetbrains.kotlin.bir.generator.childElementList
import org.jetbrains.kotlin.bir.generator.elementImplBaseType
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.Model
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

context(ImportCollector)
private fun SmartPrinter.printElementImpl(element: Element) {
    print("class ", element.elementImplName.typeName)
    print(element.params.typeParameters())

    val allFields = element.allFields
    println("(")
    withIndent {
        for (field in allFields) {
            if (field.passViaConstructorParameter) {
                printField(field, isMutable = null, inConstructor = true, kDoc = null, optInAnnotation = null)
                println(",")
            }
        }
    }
    print(")")

    val parentRefs = listOfNotNull(elementImplBaseType.takeIf { element.kind!!.isInterface }, element)
    print(
        parentRefs.joinToString(prefix = " : ") { parent ->
            parent.render() + if ((parent is ElementOrRef<*> && parent.element.typeKind == TypeKind.Class) || parent == elementImplBaseType) {
                "(${element.withArgs().render()})"
            } else {
                parent.inheritanceClauseParenthesis()
            }
        }
    )
    print(element.params.multipleUpperBoundsList())

    val body = SmartPrinter(StringBuilder()).apply {
        withIndent {
            if (element.ownerSymbolType != null) {
                printPropertyHeader("owner", element.elementImplName, false, override = true)
                println()
                withIndent {
                    println("get() = this")
                }
                println()
            }

            val childrenLists = element.walkableChildren.filterIsInstance<ListField>()
            for (field in allFields.sortedBy { it is ListField && it.isChild }) {
                if (field.isReadWriteTrackedProperty) {
                    printPropertyHeader(
                        field.backingFieldName,
                        if (field.isChild) field.typeRef.copy(nullable = true) else field.typeRef,
                        true,
                        visibility = Visibility.PRIVATE
                    )
                    if (field.initializeToThis) print(" = this") else print(" = ${field.name}")
                    println()
                }

                printField(
                    field,
                    override = true,
                    type = if (field is ListField && field.isChild) childElementListImpl.withArgs(field.baseType) else field.typeRef
                )
                if (field is ListField && field.isChild && !field.passViaConstructorParameter) {
                    print(" = ${childElementListImpl.render()}(this, ${childrenLists.indexOf(field) + 1}, ${(field.baseType as TypeRefWithNullability).nullable})")
                } else if (field.isReadWriteTrackedProperty) {
                    println()
                    withIndent {
                        print("get()")
                        printBlock {
                            println("recordPropertyRead()")
                            print("return ${field.backingFieldName}")
                            if (field.isChild && !field.nullable) {
                                print(" ?: throwChildElementRemoved(\"${field.name}\")")
                            }
                            println()
                        }
                        print("set(value)")
                        printBlock {
                            print("if (${field.backingFieldName} ${if (field.typeRef is ElementOrRef<*>) "!==" else "!="} value)")
                            printBlock {
                                if (field.isChild) {
                                    println("childReplaced(${field.backingFieldName}, value)")
                                }
                                println("${field.backingFieldName} = value")
                                println("invalidate()")
                            }
                        }
                    }
                } else {
                    if (field.initializeToThis) print(" = this") else print(" = ${field.name}")
                    println()
                }
                (field as? SingleField)?.getter?.let {
                    println()
                    withIndent {
                        println("get() = $it")
                    }
                }
                println()
            }

            if (element.walkableChildren.isNotEmpty()) {
                println()
                print("init")
                printBlock {
                    element.walkableChildren.forEach { child ->
                        if (child is SingleField) {
                            println("initChild(${child.backingFieldName})")
                        }
                    }
                }

                println()
                printFunctionDeclaration(
                    name = "acceptChildrenLite",
                    parameters = listOf(
                        FunctionParameter("visitor", elementVisitorLite),
                    ),
                    returnType = StandardTypes.unit,
                    override = true,
                )
                printBlock {
                    for (child in element.walkableChildren) {
                        when (child) {
                            is SingleField -> {
                                print(child.backingFieldName)
                                addImport(elementAcceptLite)
                                if (child.nullable) print("?")
                                println(".acceptLite(visitor)")
                            }
                            is ListField -> {
                                print(child.name)
                                println(".acceptChildrenLite(visitor)")
                            }
                        }
                    }
                }

                println()
                printFunctionWithBlockBody(
                    name = "replaceChildProperty",
                    parameters = listOf(
                        FunctionParameter("old", rootElement),
                        FunctionParameter("new", rootElement.copy(nullable = true))
                    ),
                    returnType = StandardTypes.unit,
                    override = true,
                ) {
                    print("return when")
                    printBlock {
                        for (field in element.walkableChildren) {
                            if (field is SingleField) {
                                print("this.${field.backingFieldName} === old ->")
                                printBlock {
                                    println("this.${field.backingFieldName} = new as ${field.typeRef.copy(nullable = true).render()}")
                                }
                            }
                        }
                        println("else -> throwChildForReplacementNotFound(old)")
                    }
                }
            }

            if (childrenLists.isNotEmpty()) {
                println()
                printFunctionWithBlockBody(
                    "getChildrenListById",
                    override = true,
                    parameters = listOf(FunctionParameter("id", StandardTypes.int)),
                    returnType = childElementList.withArgs(TypeRef.Star)
                ) {
                    print("return when (id)")
                    printBlock {
                        childrenLists.forEachIndexed { index, field ->
                            println("${index + 1} -> this.${field.name}")
                        }
                        println("else -> throwChildrenListWithIdNotFound(id)")
                    }
                }
            }
        }
    }.toString()

    if (body.isNotEmpty()) {
        println(" {")
        print(body.trimStart('\n'))
        print("}")
    }
    println()
}

private val childElementListImpl = type(Packages.tree, "BirImplChildElementList")
private val elementVisitorLite = type(Packages.tree, "BirElementVisitorLite")
private val elementAcceptLite = ArbitraryImportable(Packages.tree, "acceptLite")

fun printElementImpls(generationPath: File, model: Model) = model.elements.asSequence()
    .filter { it.isLeaf }
    .map { element ->
        printGeneratedType(generationPath, TREE_GENERATOR_README, element.elementImplName.packageName, element.elementImplName.typeName) {
            printElementImpl(element)
        }
    }
