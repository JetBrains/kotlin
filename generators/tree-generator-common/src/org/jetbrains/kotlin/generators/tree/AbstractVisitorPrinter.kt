/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tree

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.withIndent

abstract class AbstractVisitorPrinter<Element : AbstractElement<Element, Field, *>, Field : AbstractField<Field>>(
    val printer: ImportCollectingPrinter,
) {

    /**
     * The visitor type to print.
     */
    abstract val visitorType: ClassRef<*>

    protected open val annotations: List<Annotation>
        get() = emptyList()

    open val implementationKind: ImplementationKind
        get() = when (visitorType.kind) {
            TypeKind.Class -> ImplementationKind.AbstractClass
            TypeKind.Interface -> ImplementationKind.Interface
        }

    open val constructorParameters: List<PrimaryConstructorParameter>
        get() = emptyList()

    open val optIns: List<ClassRef<*>>
        get() = emptyList()

    /**
     * The result type parameter of the visitor. All visitor methods return result of this type.
     */
    protected val resultTypeVariable = TypeVariable("R", emptyList(), Variance.OUT_VARIANCE)

    /**
     * The data type parameter of the visitor. ALl visitor methods accept a parameter of this type.
     */
    protected val dataTypeVariable = TypeVariable("D", emptyList(), Variance.IN_VARIANCE)

    /**
     * The type parameters of the visitor class. Void visitors have no type parameters,
     * regular visitors usually have [resultTypeVariable] and [dataTypeVariable] here.
     */
    abstract val visitorTypeParameters: List<TypeVariable>

    abstract val visitorDataType: TypeRef

    abstract fun visitMethodReturnType(element: Element): TypeRef

    /**
     * The superclass for this visitor class.
     */
    abstract val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>

    /**
     * If `true`, visitor methods for generic tree elements will be parameterized correspondingly.
     * Otherwise, type arguments of generic tree elements will be replaced with `*`.
     */
    open val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    /**
     * Allows to customize the default element to visit if the method for visiting this [element] is not overridden.
     *
     * If returns `null`, methods for this element will not be overridden in this visitor class (except the root element).
     */
    open fun parentInVisitor(element: Element): Element? = element.parentInVisitor

    open fun skipElement(element: Element): Boolean = false

    /**
     * Prints a single visitor method declaration, without body.
     */
    protected fun ImportCollectingPrinter.printVisitMethodDeclaration(
        element: Element,
        hasDataParameter: Boolean = true,
        modality: Modality? = null,
        override: Boolean = false,
        returnType: TypeRef = visitMethodReturnType(element),
    ) {
        val visitorParameterType = ElementRef(
            element,
            element.params.associateWith { if (allowTypeParametersInVisitorMethods) it else TypeRef.Star }
        )
        val parameters = buildList {
            add(FunctionParameter(element.visitorParameterName, visitorParameterType))
            if (hasDataParameter) add(FunctionParameter("data", visitorDataType))
        }
        printFunctionDeclaration(
            name = element.visitFunctionName,
            parameters = parameters,
            returnType = returnType,
            typeParameters = if (allowTypeParametersInVisitorMethods) {
                element.params
            } else {
                emptyList()
            },
            modality = modality,
            override = override,
        )
    }

    protected open fun visitMethodModality(element: Element): Modality? {
        val parentInVisitor = parentInVisitor(element)
        return when {
            visitorSuperTypes.isEmpty() && parentInVisitor == null && visitorType.kind == TypeKind.Class -> Modality.ABSTRACT
            visitorSuperTypes.isEmpty() && parentInVisitor != null && visitorType.kind == TypeKind.Class -> Modality.OPEN
            else -> null
        }
    }

    protected open fun overrideVisitMethod(element: Element): Boolean =
        parentInVisitor(element) != null && visitorSuperTypes.isNotEmpty()

    protected open fun printMethodsForElement(element: Element) {
        printer.run {
            val parentInVisitor = parentInVisitor(element)
            if (parentInVisitor == null && !element.isRootElement) return
            println()
            printVisitMethodDeclaration(
                element,
                modality = visitMethodModality(element),
                override = overrideVisitMethod(element)
            )
            if (parentInVisitor != null) {
                println(" =")
                withIndent {
                    print(parentInVisitor.visitFunctionName, "(", element.visitorParameterName, ", data)")
                }
            }
            println()
        }
    }

    protected open fun ImportCollectingPrinter.printAdditionalMethods() {
    }

    protected open val ImportCollecting.classKDoc: String
        get() = ""

    open fun printVisitor(elements: List<Element>) {
        val visitorType = this.visitorType
        printer.run {
            printKDoc(
                buildString {
                    val classKDoc = classKDoc
                    if (classKDoc.isNotBlank()) {
                        append(classKDoc.trim())
                        appendLine()
                        appendLine()
                    }
                    append("Auto-generated by [${this@AbstractVisitorPrinter::class.qualifiedName}]")
                }
            )
            for (annotation in annotations) {
                printAnnotation(annotation)
            }
            optIns.forEach { println("@OptIn(", it.render(), "::class)") }
            print(implementationKind.title, " ")
            print(visitorType.simpleName, visitorTypeParameters.typeParameters())
            if (constructorParameters.isNotEmpty()) {
                println("(")
                withIndent {
                    for (parameter in constructorParameters) {
                        printPropertyDeclaration(
                            name = parameter.name,
                            type = parameter.type,
                            kind = parameter.kind,
                            inConstructor = true,
                            visibility = parameter.visibility,
                            initializer = parameter.defaultValue
                        )
                        println()
                    }
                }
                print(")")
            }
            printInheritanceClause(visitorSuperTypes)
            print(visitorTypeParameters.multipleUpperBoundsList())
            printBlock {
                printAdditionalMethods()
                for (element in elements) {
                    if (skipElement(element)) continue
                    printMethodsForElement(element)
                }
            }
        }
    }
}
