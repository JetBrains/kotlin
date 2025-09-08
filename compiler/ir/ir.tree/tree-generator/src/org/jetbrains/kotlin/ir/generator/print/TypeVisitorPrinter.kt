/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.FunctionParameter
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.printer.printFunctionDeclaration
import org.jetbrains.kotlin.generators.tree.printer.printKDoc
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.SimpleField
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.withIndent

internal open class TypeVisitorPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
    protected val rootElement: Element,
) : AbstractVisitorPrinter<Element, Field>(printer) {

    companion object {

        @JvmStatic
        protected fun ImportCollectingPrinter.printVisitTypeKDoc() {
            printKDoc(
                """
                A customization point called by [visitTypeRecursively] on each field of [container] that contains
                an [${irTypeType.render()}], as well as on all the latter's type arguments (for [${irSimpleTypeType.render()}]s).
                """.trimIndent()
            )
        }

        @JvmStatic
        protected fun ImportCollectingPrinter.printVisitTypeRecursivelyKDoc() {
            printKDoc(
                """
                Called on each field of [container] that contains an [${irTypeType.render()}].
                The default implementation calls [visitType] for [type] and each of its type arguments
                (for [${irSimpleTypeType.render()}]s).
                """.trimIndent()
            )
        }
    }

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irVisitorType.withArgs(resultTypeVariable, dataTypeVariable))

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element): TypeRef = resultTypeVariable

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    protected fun Element.getFieldsWithIrTypeType(insideParent: Boolean = false): List<Field> {
        val parentsFields = elementParents.flatMap { it.element.getFieldsWithIrTypeType(insideParent = true) }
        if (insideParent && this.parentInVisitor != null) {
            return parentsFields
        }

        val irTypeFields = this.fields
            .filter {
                val type = when (it) {
                    is SimpleField -> it.typeRef
                    is ListField -> it.baseType
                }
                type.toString() == irTypeType.toString()
            }

        return irTypeFields + parentsFields
    }

    protected fun ImportCollectingPrinter.printVisitTypeMethod(
        name: String,
        hasDataParameter: Boolean,
        modality: Modality?,
        override: Boolean,
    ) {
        printFunctionDeclaration(
            name = name,
            parameters = listOfNotNull(
                FunctionParameter("container", rootElement),
                FunctionParameter("type", irTypeType),
                FunctionParameter("data", visitorDataType).takeIf { hasDataParameter },
            ),
            returnType = StandardTypes.unit,
            modality = modality,
            override = override
        )
    }

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        printVisitTypeKDoc()
        printVisitTypeMethod(name = "visitType", hasDataParameter = true, modality = Modality.ABSTRACT, override = false)
        println()
        println()
        printVisitTypeRecursivelyKDoc()
        printVisitTypeRecursively()
        println()
        printVisitAnnotationUsage(hasDataParameter = true, modality = Modality.OPEN, override = false)
    }

    protected fun ImportCollectingPrinter.printVisitTypeRecursively(hasDataParameter: Boolean = true) {
        printVisitTypeMethod(name = "visitTypeRecursively", hasDataParameter, modality = Modality.OPEN, override = false)
        printBlock {
            val data = runIf(hasDataParameter) { ", data" } ?: ""
            printlnMultiLine(
                """
                visitType(container, type$data)
                type.annotations.forEach { visitAnnotationUsage(it$data) }
                if (type is ${irSimpleTypeType.render()}) {
                    type.arguments.forEach {
                        if (it is ${irTypeProjectionType.render()}) {
                            visitTypeRecursively(container, it.type$data)
                        }
                    }
                }
                """
            )
        }
    }

    protected fun ImportCollectingPrinter.printVisitAnnotationUsageDeclaration(
        hasDataParameter: Boolean,
        modality: Modality?,
        override: Boolean,
    ) {
        printFunctionDeclaration(
            name = "visitAnnotationUsage",
            parameters = listOfNotNull(
                FunctionParameter("annotationUsage", IrTree.constructorCall),
                FunctionParameter("data", visitorDataType).takeIf { hasDataParameter },
            ),
            returnType = StandardTypes.unit,
            modality = modality,
            override = override,
        )
    }

    protected fun ImportCollectingPrinter.printVisitAnnotationUsage(
        hasDataParameter: Boolean,
        modality: Modality,
        override: Boolean,
    ) {
        printVisitAnnotationUsageDeclaration(hasDataParameter, modality, override)
        printBlock {
            val data = runIf(hasDataParameter) { ", data" } ?: ""
            println("visitElement(annotationUsage$data)")
            println("visitTypeRecursively(annotationUsage, annotationUsage.type$data)")
        }
    }

    protected fun ImportCollectingPrinter.printTypeRemappings(
        element: Element,
        irTypeFields: List<Field>,
        hasDataParameter: Boolean,
        transformTypes: Boolean = false,
    ) {
        printTypeRemappingsOverridable(this, element, irTypeFields, hasDataParameter, transformTypes)
    }

    protected open fun printTypeRemappingsOverridable(
        printer: ImportCollectingPrinter,
        element: Element,
        irTypeFields: List<Field>,
        hasDataParameter: Boolean,
        transformTypes: Boolean = false,
    ): Unit = with(printer) {
        val visitTypeMethodName = if (transformTypes) "transformTypeRecursively" else "visitTypeRecursively"
        val visitorParam = element.visitorParameterName
        fun addVisitTypeStatement(field: Field) {
            val access = "$visitorParam.${field.name}"
            when (field) {
                is SimpleField -> {
                    if (transformTypes) {
                        print(access, " = ", visitTypeMethodName, "(", visitorParam, ", ", access)
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println(")")
                    } else {
                        val argumentToPassToVisitType = if (field.nullable) {
                            print(access, "?.let { ")
                            "it"
                        } else {
                            access
                        }
                        print(visitTypeMethodName, "(", visitorParam, ", ", argumentToPassToVisitType)
                        if (hasDataParameter) {
                            print(", data")
                        }
                        print(")")
                        if (field.nullable) {
                            print(" }")
                        }
                        println()
                    }
                }
                is ListField -> {
                    if (transformTypes) {
                        when (field.mutability) {
                            ListField.Mutability.Var -> {
                                addImport(ArbitraryImportable("org.jetbrains.kotlin.utils", "memoryOptimizedMap"))
                                print(access, " = ", access, ".memoryOptimizedMap")
                                printBlock {
                                    print(visitTypeMethodName, "(", visitorParam, ", it")
                                    if (hasDataParameter) {
                                        print(", data")
                                    }
                                    println(")")
                                }
                            }
                            ListField.Mutability.MutableList, ListField.Mutability.Array -> {
                                print(access, ".replaceAll")
                                printBlock {
                                    print(visitTypeMethodName, "(", visitorParam, ", it")
                                    if (hasDataParameter) {
                                        print(", data")
                                    }
                                    println(")")
                                }
                            }
                        }

                    } else {
                        print(access, ".forEach { ", visitTypeMethodName, "(", visitorParam, ", it")
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println(") }")
                    }
                }
            }
        }
        when (element) {
            IrTree.memberAccessExpression -> {
                if (irTypeFields.singleOrNull()?.name != "typeArguments") {
                    error(
                        """`${IrTree.memberAccessExpression.typeName}` has unexpected fields with `IrType` type. 
                                        |Please adjust logic of `${visitorType.simpleName}`'s generation.""".trimMargin()
                    )
                }
                if (transformTypes) {
                    print(visitorParam, ".typeArguments.replaceAll")
                    printBlock {
                        print(visitTypeMethodName, "(", visitorParam, ", it")
                        if (hasDataParameter) {
                            print(", data")
                        }
                        println(")")
                    }
                } else {
                    println("for (type in ${visitorParam}.typeArguments) {")
                    withIndent {
                        println("if (type != null) {")
                        withIndent {
                            print(visitTypeMethodName, "(", visitorParam, ", type")
                            if (hasDataParameter) {
                                print(", data")
                            }
                            println(")")
                        }
                        println("}")
                    }
                    println("}")
                }
            }
            IrTree.`class` -> {
                if (transformTypes) {
                    print(visitorParam, ".valueClassRepresentation = ")
                }
                println(visitorParam, ".valueClassRepresentation?.mapUnderlyingType {")
                withIndent {
                    print(visitTypeMethodName, "(", visitorParam, ", it")
                    if (hasDataParameter) {
                        print(", data")
                    }
                    println(")")
                    if (!transformTypes) {
                        println("it")
                    }
                }
                println("}")
                irTypeFields.forEach(::addVisitTypeStatement)
            }
            else -> {
                irTypeFields.forEach(::addVisitTypeStatement)
            }
        }
    }

    override fun printMethodsForElement(element: Element) {
        val irTypeFields = element.getFieldsWithIrTypeType()
        val isAnnotationContainer = element in listOf(IrTree.declarationBase, IrTree.file)
        if (!isAnnotationContainer && (irTypeFields.isEmpty() || element.parentInVisitor == null)) return

        printer.run {
            println()
            printVisitMethodDeclaration(
                element = element,
                override = true,
            )
            printBlock {
                printTypeRemappings(element, irTypeFields, hasDataParameter = true)
                if (isAnnotationContainer) {
                    println("${element.visitorParameterName}.annotations.forEach { visitAnnotationUsage(it, data) }")
                }
                println(
                    "return super.",
                    element.visitFunctionName,
                    "(",
                    element.visitorParameterName,
                    ", data)"
                )
            }
        }
    }
}
