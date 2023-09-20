/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractVisitorPrinter<Element : AbstractElement<Element, Field>, Field : AbstractField>(
    val printer: SmartPrinter,
    val visitSuperTypeByDefault: Boolean,
) {
    abstract val visitorType: ClassRef<*>

    protected val resultTypeVariable = TypeVariable("R", emptyList(), Variance.OUT_VARIANCE)

    protected val dataTypeVariable = TypeVariable("D", emptyList(), Variance.IN_VARIANCE)

    abstract val visitorTypeParameters: List<TypeVariable>

    abstract val visitorSuperType: ClassRef<PositionTypeParameterRef>?

    abstract val allowTypeParametersInVisitorMethods: Boolean

    open fun parentInVisitor(element: Element): Element? = element.parentInVisitor

    protected fun SmartPrinter.printVisitMethodDeclaration(
        element: Element,
        additionalParameters: List<Pair<String, TypeRef>>,
        returnType: TypeRef,
        modality: Modality = Modality.FINAL,
        override: Boolean = false,
    ) {
        val visitorParameterType = ElementRef(
            element,
            element.params.associateWith { if (allowTypeParametersInVisitorMethods) it else TypeRef.Star }
        )
        printFunctionDeclaration(
            name = element.visitFunctionName,
            parameters = listOf(element.visitorParameterName to visitorParameterType) + additionalParameters,
            returnType = returnType,
            typeParameters = if (allowTypeParametersInVisitorMethods) {
                element.params
            } else {
                emptyList()
            },
            modality = modality,
            override = override,
        )
        print(
            element.multipleUpperBoundsList(),
        )
    }

    protected fun printMethodDeclarationForElement(element: Element, modality: Modality, override: Boolean) {
        val parentVisitorType = this@AbstractVisitorPrinter.visitorSuperType
        val resultType = parentVisitorType?.args?.get(PositionTypeParameterRef(0)) ?: visitorTypeParameters[0]
        val dataType = parentVisitorType?.args?.get(PositionTypeParameterRef(1)) ?: visitorTypeParameters[1]
        printer.run {
            println()
            printVisitMethodDeclaration(
                element,
                additionalParameters = listOf("data" to dataType),
                returnType = resultType,
                modality = modality,
                override = override
            )
        }
    }

    protected open fun printMethodsForElement(element: Element) {
        val isInterface = visitorType.kind == TypeKind.Interface
        printer.run {
            val parentInVisitor = parentInVisitor(element)
            if (parentInVisitor == null && !element.isRootElement) return
            printMethodDeclarationForElement(
                element,
                modality = when {
                    parentInVisitor == null && !isInterface -> Modality.ABSTRACT
                    parentInVisitor != null && !isInterface -> Modality.OPEN
                    else -> Modality.FINAL
                },
                override = parentInVisitor != null && visitorSuperType != null,
            )
            if (parentInVisitor != null) {
                print(" = ", parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ", data)")
            }
            println()
        }
    }

    fun printVisitor(elements: List<Element>) {
        val visitorType = this.visitorType
        printer.run {
            when (visitorType.kind) {
                TypeKind.Interface -> print("interface ")
                TypeKind.Class -> print("abstract class ")
            }
            print(visitorType.simpleName, visitorTypeParameters.generics, " ")
            visitorSuperType?.let {
                print(": ", it.typeWithArguments, it.inheritanceClauseParenthesis(), " ")
            }
            println("{")
            withIndent {
                for (element in elements) {
                    if (element.isRootElement && visitSuperTypeByDefault) continue
                    if (visitSuperTypeByDefault && parentInVisitor(element) == null) continue
                    printMethodsForElement(element)
                }
            }
            println("}")
        }
    }
}