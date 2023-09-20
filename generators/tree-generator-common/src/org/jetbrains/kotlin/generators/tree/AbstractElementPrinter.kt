/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

/**
 * A common class for printing FIR or IR tree elements.
 */
abstract class AbstractElementPrinter<Element : AbstractElement<*, Field>, Field : AbstractField>(
    private val printer: SmartPrinter,
) {
    protected abstract val fieldPrinter: AbstractFieldPrinter<Field>

    protected abstract val visitorType: ClassRef<*>

    protected abstract val transformerType: ClassRef<*>

    protected abstract fun SmartPrinter.printAdditionalMethods(element: Element)

    private fun SmartPrinter.printTypeVariable(typeVariable: TypeVariable) {
        print(typeVariable.name)
        if (typeVariable.bounds.size > 1) TODO("Printing multiple upper bounds is not implemented")
        typeVariable.bounds.singleOrNull()?.let {
            print(" : ", it.typeWithArguments)
        }
    }

    protected fun SmartPrinter.printAcceptMethod(element: Element, hasImplementation: Boolean, kDoc: String?) {
        if (!element.hasAcceptMethod) return
        println()
        if (element.isRootElement) {
            printKDoc(kDoc)
        }
        if (!element.isRootElement) {
            print("override ")
        }
        print("fun <R, D> accept(visitor: ${visitorType.simpleName}<R, D>, data: D): R")
        if (hasImplementation) {
            println(" =")
            withIndent {
                print("visitor.visit${element.name}(this, data)")
            }
        }
        println()
    }

    protected fun SmartPrinter.printTransformMethod(element: Element, hasImplementation: Boolean, returnType: TypeRef, kDoc: String?) {
        if (!element.hasTransformMethod) return
        println()
        if (element.isRootElement) {
            printKDoc(kDoc)
        }
        if (returnType is TypeParameterRef && hasImplementation) {
            println("@Suppress(\"UNCHECKED_CAST\")")
        }
        if (!element.isRootElement) {
            print("override ")
        }
        print("fun <")
        if (returnType is TypeVariable) {
            printTypeVariable(returnType)
            print(", ")
        }
        print("D> transform(transformer: ${transformerType.simpleName}<D>, data: D): ", returnType.typeWithArguments)
        if (hasImplementation) {
            println(" =")
            withIndent {
                println("transformer.transform${element.name}(this, data) as ", returnType.typeWithArguments)
            }
        } else {
            println()
        }
    }

    protected fun SmartPrinter.printAcceptChildrenMethod(element: Element, visitorResultType: TypeRef, kDoc: String?) {
        if (!element.hasAcceptChildrenMethod) return
        println()
        if (element.isRootElement) {
            printKDoc(kDoc)
        }
        print("fun <")
        if (visitorResultType is TypeVariable) {
            printTypeVariable(visitorResultType)
            print(", ")
        }
        print("D> acceptChildren(visitor: ${visitorType.simpleName}<", visitorResultType.typeWithArguments, ", D>, data: D)")
        println()
    }

    protected fun SmartPrinter.printTransformChildrenMethod(element: Element, returnType: TypeRef, kDoc: String?) {
        if (!element.hasTransformChildrenMethod) return
        println()
        if (element.isRootElement) {
            printKDoc(kDoc)
        }
        print("fun <D> transformChildren(transformer: ${transformerType.simpleName}<D>, data: D)")
        if (returnType != StandardTypes.unit) {
            print(": ", returnType.typeWithArguments)
        }
        println()
    }

    fun printElement(element: Element) {
        printer.run {
            val kind = element.kind ?: error("Expected non-null element kind")
            fun abstract() {
                if (!kind.isInterface) {
                    print("abstract ")
                }
            }

            printKDoc(element.kDoc)
            print("${kind.title} ${element.type}")
            print(element.typeParameters())

            if (element.parentRefs.isNotEmpty()) {
                print(" : ")
                print(
                    element.parentRefs.sortedBy { it.typeKind }.joinToString(", ") { parent ->
                        var result = parent.type
                        if (parent is ParametrizedTypeRef<*, *> && parent.args.isNotEmpty()) {
                            result += parent.args.values.joinToString(", ", "<", ">") { it.typeWithArguments }
                        }
                        result + parent.inheritanceClauseParenthesis()
                    },
                )
            }
            print(element.multipleUpperBoundsList())
            println(" {")
            withIndent {
                for (field in element.allFields) {
                    if (field.isFinal && field.fromParent || field.isParameter) continue
                    println()
                    fieldPrinter.printField(field, isImplementation = false, override = field.fromParent) {
                        if (!field.isFinal) {
                            abstract()
                        }
                    }
                }
                printAdditionalMethods(element)
            }
            println("}")
        }
    }
}

fun AbstractElement<*, *>.multipleUpperBoundsList(): String {
    val paramsWithMultipleUpperBounds = params.filter { it.bounds.size > 1 }.takeIf { it.isNotEmpty() } ?: return ""
    return buildString {
        append(" where ")
        paramsWithMultipleUpperBounds.joinTo(this, separator = ", ") { param ->
            param.bounds.joinToString(", ") { bound -> "$param : ${bound.typeWithArguments}" }
        }
        append("")
    }
}