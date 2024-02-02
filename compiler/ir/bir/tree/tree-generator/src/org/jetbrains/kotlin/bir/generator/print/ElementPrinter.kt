/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.print

import org.jetbrains.kotlin.bir.generator.*
import org.jetbrains.kotlin.bir.generator.BirTree.body
import org.jetbrains.kotlin.bir.generator.Model
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.Field
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

private val elementAccept = ArbitraryImportable(Packages.tree, "accept")

fun ImportCollectingPrinter.printElement(element: Element) {
    val kind = element.kind ?: error("Expected non-null element kind")

    print(kind.title, " ", element.typeName)
    print(element.params.typeParameters())
    if (element.kind == ImplementationKind.AbstractClass) {
        print("()")
    }

    val parentRefs = element.parentRefs
    if (parentRefs.isNotEmpty()) {
        print(
            parentRefs.sortedBy { it.typeKind }.joinToString(prefix = " : ") { parent ->
                parent.render() + parent.inheritanceClauseParenthesis()
            }
        )
    }
    print(element.params.multipleUpperBoundsList())

    val printer = SmartPrinter(StringBuilder())
    this@printElement.withNewPrinter(printer) {
        withIndent {
            for (field in element.fields) {
                printField(
                    field,
                    false,
                    override = field.isOverride,
                    modality = Modality.ABSTRACT.takeIf { !kind.isInterface },
                )
                println()
            }

            if (element.implementations.isNotEmpty() && element.walkableChildren.isNotEmpty()) {
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
                println()
            }

            if (element.implementations.isNotEmpty()) {
                printFunctionDeclaration("getElementClassInternal", listOf(), elementClassType.withArgs(TypeRef.Star), override = true, optInAnnotation = birImplementationDetailType)
                println(" = ${element.withArgs().render()}")
                println()
            }

            print("companion object : ${elementClassType.render()}")
            print("<${element.withStarArgs().render()}>(")
            print("${element.withArgs().render()}::class.java")
            print(", ${element.classId}")
            print(", ${element.implementations.isNotEmpty()}")
            println(")")
        }
    }

    val body = printer.toString()
    if (body.isNotEmpty()) {
        println(" {")
        print(body.trimStart('\n'))
        print("}")
    }
    println()

    addAllImports(element.additionalImports)
}

fun ImportCollectingPrinter.printField(
    field: Field,
    inImplementation: Boolean,
    type: TypeRef = field.typeRef,
    kind: VariableKind = if (field.isMutable) VariableKind.VAR else VariableKind.VAL,
    override: Boolean = field.isOverride,
    modality: Modality? = null,
    visibility: Visibility = Visibility.PUBLIC,
    inConstructor: Boolean = false,
) {
    val defaultValue = if (inImplementation)
        field.implementationDefaultStrategy as? AbstractField.ImplementationDefaultStrategy.DefaultValue
    else null
    printPropertyDeclaration(
        name = field.name,
        type = type,
        kind = kind,
        inConstructor = inConstructor,
        visibility = field.visibility,
        modality = modality,
        override = override,
        isLateinit = (inImplementation) && field.implementationDefaultStrategy is AbstractField.ImplementationDefaultStrategy.Lateinit,
        isVolatile = (inImplementation) && field.isVolatile,
        initializer = defaultValue?.takeUnless { it.withGetter }?.defaultValue
    )
    println()

    if (defaultValue != null && defaultValue.withGetter) {
        withIndent {
            println("get() = ${defaultValue.defaultValue}")
        }
    }

    field.customSetter?.let {
        withIndent {
            print("set(value)")
            printBlock {
                println(it)
            }
        }
    }
}
