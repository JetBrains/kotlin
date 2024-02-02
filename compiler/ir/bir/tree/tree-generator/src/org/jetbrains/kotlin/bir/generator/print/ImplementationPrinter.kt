/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.bir.generator.print

import org.jetbrains.kotlin.bir.generator.BirTree.rootElement
import org.jetbrains.kotlin.bir.generator.Packages
import org.jetbrains.kotlin.bir.generator.childElementList
import org.jetbrains.kotlin.bir.generator.elementImplBaseType
import org.jetbrains.kotlin.bir.generator.model.Field
import org.jetbrains.kotlin.bir.generator.model.Implementation
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

fun ImportCollectingPrinter.printElementImplementation(implementation: Implementation) {
    val element = implementation.element

    print("class ", implementation.typeName)
    print(element.params.typeParameters())

    val allFields = implementation.allFields

    val fieldsInFullCtor = allFields.filter { field -> field.isInFullCtor }
    val fieldsInDefaultCtor =
        allFields.filter { field -> field.implementationDefaultStrategy is AbstractField.ImplementationDefaultStrategy.Required }

    println("(")
    withIndent {
        fieldsInFullCtor.forEachIndexed { _, field ->
            print(field.name, ": ", field.typeRef.render())
            println(",")
        }
    }
    print(")")

    val parentRefs = listOfNotNull(
        elementImplBaseType.takeIf { element.kind!!.isInterface },
        element.withSelfArgs(),
        //element.elementAncestorsAndSelfDepthFirst().firstNotNullOfOrNull { it.ownerSymbolType },
        element.ownerSymbolType,
    )
    print(
        parentRefs.joinToString(prefix = " : ") { parent ->
            parent.render() + parent.inheritanceClauseParenthesis()
        }
    )
    print(element.params.multipleUpperBoundsList())

    val printer = SmartPrinter(StringBuilder())
    this@printElementImplementation.withNewPrinter(printer) {
        withIndent {
            if (fieldsInDefaultCtor.size != fieldsInFullCtor.size) {
                println("constructor(")
                withIndent {
                    fieldsInDefaultCtor.forEachIndexed { _, field ->
                        print(field.name, ": ", field.typeRef.render())
                        println(",")
                    }
                }
                println(") : this(")
                withIndent {
                    fieldsInFullCtor.forEachIndexed { _, field ->
                        print(field.name, " = ")

                        val defaultValue = field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
                        if (defaultValue != null) {
                            print(defaultValue.defaultValue)
                        } else {
                            print(field.name)
                        }

                        println(",")
                    }
                }
                println(")")
                println()
            }

            if (element.ownerSymbolType != null) {
                printPropertyDeclaration("owner", element.elementImplName, VariableKind.VAL, override = true)
                println()
                withIndent {
                    println("get() = this")
                }
                println()

                printPropertyDeclaration("isBound", type<Boolean>(), VariableKind.VAL, override = true)
                println()
                withIndent {
                    println("get() = true")
                }
                println()
            }

            val childrenLists = allFields.filterIsInstance<ListField>().filter { it.isChild }
            allFields.sortedBy { it is ListField && it.isChild }.forEach { field ->
                val defaultValue = field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
                val isLateinit = field.implementationDefaultStrategy is AbstractField.ImplementationDefaultStrategy.Lateinit

                val hasBackingField = isLateinit || field.isReadWriteTrackedProperty
                if (hasBackingField) {
                    printPropertyDeclaration(
                        name = field.backingFieldName,
                        type = if (field.isChild || isLateinit) field.typeRef.copy(nullable = true) else field.typeRef,
                        kind = if (field.isMutable) VariableKind.VAR else VariableKind.VAL,
                        visibility = Visibility.PRIVATE,
                        initializer = when {
                            field.isInFullCtor -> field.name
                            defaultValue != null && !defaultValue.withGetter -> defaultValue.defaultValue
                            isLateinit -> "null"
                            else -> null
                        }
                    )
                    println()
                }

                printPropertyDeclaration(
                    name = field.name,
                    type = if (field is ListField && field.isChild) childElementListImpl.withArgs(field.baseType) else field.typeRef,
                    kind = if (field.isMutable) VariableKind.VAR else VariableKind.VAL,
                    visibility = field.visibility,
                    override = true,
                    initializer = when {
                        hasBackingField -> null
                        field is ListField && field.isChild -> "${childElementListImpl.render()}(this, ${childrenLists.indexOf(field) + 1}, ${(field.baseType as TypeRefWithNullability).nullable})"
                        field.isInFullCtor -> field.name
                        defaultValue != null && !defaultValue.withGetter -> defaultValue.defaultValue
                        else -> null
                    }
                )
                println()

                if (field.isReadWriteTrackedProperty) {
                    withIndent {
                        print("get()")
                        printBlock {
                            println("recordPropertyRead()")
                            print("return ${field.backingFieldName}")
                            if (field.isChild && !field.nullable) {
                                print(" ?: throwChildElementRemoved(\"${field.name}\")")
                            } else if(isLateinit) {
                                print(" ?: throwLateinitPropertyUninitialized(\"${field.name}\")")
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
                }

                if (defaultValue != null && defaultValue.withGetter) {
                    withIndent {
                        println("get() = ${defaultValue.defaultValue}")
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
                                //if (child.nullable)
                                print("?")
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

            implementation.generationCallback?.invoke(this)
        }
    }

    val body = printer.toString()
    if (body.isNotEmpty()) {
        println(" {")
        print(body.trimStart('\n'))
        print("}")
    }
    println()

    addAllImports(implementation.additionalImports)
}

private val Field.isInFullCtor: Boolean
    get() = implementationDefaultStrategy.let {
        it is AbstractField.ImplementationDefaultStrategy.Required || it is AbstractField.ImplementationDefaultStrategy.DefaultValue && !it.withGetter && it.defaultValue != "this"
    } && !(this is ListField && isChild)

private val childElementListImpl = type(Packages.tree, "BirImplChildElementList")
private val elementVisitorLite = type(Packages.tree, "BirElementVisitorLite")
private val elementAcceptLite = ArbitraryImportable(Packages.tree, "acceptLite")