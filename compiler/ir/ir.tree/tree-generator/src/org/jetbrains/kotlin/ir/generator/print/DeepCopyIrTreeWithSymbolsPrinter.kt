/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.generators.util.printBlock
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.model.symbol.symbolRemapperMethodName
import org.jetbrains.kotlin.utils.withIndent

internal class DeepCopyIrTreeWithSymbolsPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element): TypeRef = element

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(irDeepCopyBaseType)

    override val optIns: List<ClassRef<*>> = listOf(irImplementationDetailType)

    override val implementationKind: ImplementationKind
        get() = ImplementationKind.OpenClass

    private val symbolRemapperParameter = PrimaryConstructorParameter(
        FunctionParameter("symbolRemapper", symbolRemapperType),
        VariableKind.VAL,
        Visibility.PRIVATE
    )

    private val typeRemapperParameter = PrimaryConstructorParameter(
        FunctionParameter("typeRemapper", typeRemapperType.copy(nullable = true), "null"),
        VariableKind.PARAMETER
    )

    override val constructorParameters: List<PrimaryConstructorParameter> = listOf(symbolRemapperParameter, typeRemapperParameter)

    override fun ImportCollectingPrinter.printAdditionalMethods() {
        addImport(ArbitraryImportable("org.jetbrains.kotlin.utils", "memoryOptimizedMap"))

        printPropertyDeclaration(
            name = "transformedModule",
            type = IrTree.moduleFragment.copy(nullable = true),
            kind = VariableKind.VAR,
            visibility = Visibility.PRIVATE,
            initializer = "null",
        )
        println()
        printPropertyDeclaration(
            name = "typeRemapper",
            type = typeRemapperType,
            kind = VariableKind.VAL,
            visibility = Visibility.PRIVATE,
            initializer = "${typeRemapperParameter.name} ?: ${deepCopyTypeRemapperType.render()}(${symbolRemapperParameter.name})",
        )
        println()
        printPropertyDeclaration(
            name = "transformedLoops",
            type = StandardTypes.hashMap.withArgs(IrTree.loop, IrTree.loop),
            kind = VariableKind.VAL,
            visibility = Visibility.PRIVATE,
            initializer = "${StandardTypes.hashMap.render()}()",
        )
        println()
        println()
        printInitBlock()
        println()
        println("override fun ${irTypeType.render()}.remapType() = typeRemapper.remapType(this)")
    }

    private fun ImportCollectingPrinter.printInitBlock() {
        print("init")
        printBlock {
            println("// TODO refactor")
            println("// After removing usages of ${deepCopyTypeRemapperType.simpleName} constructor from compose, the lateinit property `${deepCopyTypeRemapperType.simpleName}.deepCopy`")
            println("// can be refactored to a constructor parameter.")
            print("(this.${typeRemapperParameter.name} as? ${deepCopyTypeRemapperType.render()})?.let")
            printBlock {
                println("it.deepCopy = this")
            }
        }
    }

    override fun printMethodsForElement(element: Element) {
        if (!element.generateVisitorMethod) return

        printer.run {
            if (element.isRootElement) {
                println()
                printVisitMethodDeclaration(element, hasDataParameter = false, override = true)
                println(" =")
                withIndent {
                    println("throw IllegalArgumentException(\"Unsupported element type: $${element.visitorParameterName}\")")
                }
                return
            }

            if (element.implementations.isEmpty()) return

            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, override = true)

            println(" =")
            withIndent {
                val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                print(implementation.render())
                if (useWithShapeConstructor(element)) {
                    print("WithShape")
                }
                val constructorArguments: List<Field> = implementation.fieldsInConstructor.filter { !it.deepCopyExcludeFromConstructor }
                println("(")
                withIndent {
                    if (implementation.hasConstructorIndicator && !useWithShapeConstructor(element)) {
                        println("constructorIndicator = null,")
                    }
                    for (field in constructorArguments) {
                        print(field.name, " = ")
                        copyField(element, field)
                        println(",")
                    }
                    if (useWithShapeConstructor(element)) {
                        printWithShapeExtraArguments(element)
                    }
                }
                val fieldsInApply = implementation.fieldsInBody.filter { !it.deepCopyExcludeFromApply && it !in constructorArguments }
                printApply(element, fieldsInApply)
            }
        }
    }

    private fun ImportCollectingPrinter.copyField(element: Element, field: Field) {
        if (field is ListField) {
            print(element.visitorParameterName, ".", field.name, field.call(), "memoryOptimizedMap")
            print(" { ")
            copyValue(field, "it")
            print(" }")
        } else {
            copyValue(field, element.visitorParameterName, ".", field.name)
        }
    }

    private fun ImportCollectingPrinter.copyValue(field: Field, vararg valueArgs: Any?) {
        val typeRef = if (field is ListField) {
            field.baseType
        } else {
            field.typeRef
        }
        val symbolFieldClass = field.symbolClass
        val safeCall = if (typeRef.nullable) "?." else "."
        when {
            typeRef !is ClassOrElementRef -> {
                print(*valueArgs)
            }
            symbolFieldClass != null -> {
                val symbolRemapperFunction =
                    symbolRemapperMethodName(symbolFieldClass, field.symbolFieldRole ?: AbstractField.SymbolFieldRole.REFERENCED)
                if (typeRef.nullable) {
                    print(*valueArgs, "?.let(", symbolRemapperParameter.name, "::", symbolRemapperFunction, ")")
                } else {
                    print(symbolRemapperParameter.name, ".", symbolRemapperFunction, "(", *valueArgs, ")")
                }
            }
            typeRef.isSameClassAs(IrTree.loop) -> {
                print("transformedLoops.getOrDefault(", *valueArgs, ", ", *valueArgs, ")")
            }
            typeRef is ElementOrRef<*> -> {
                print(*valueArgs, safeCall, "transform()")
            }
            typeRef.isSameClassAs(irTypeType) -> {
                print(*valueArgs, safeCall, "remapType()")
            }
            typeRef.isSameClassAs(type<ValueClassRepresentation<*>>()) -> {
                addImport(irSimpleTypeType)
                print(*valueArgs, safeCall, "mapUnderlyingType { it.remapType() as IrSimpleType }")
            }
            typeRef is ClassRef<*> -> {
                print(*valueArgs)
            }
        }
    }

    private fun useWithShapeConstructor(element: Element): Boolean =
        element.isSubclassOfAny(IrTree.functionAccessExpression, IrTree.functionReference, IrTree.propertyReference)

    private fun ImportCollectingPrinter.printApply(element: Element, applyFields: List<Field>) {
        print(").apply")
        printBlock {
            if (element.isSubclassOf(IrTree.declaration) && !element.isSubclassOf(IrTree.variable)) {
                println("with(factory) { declarationCreated() }")
            }
            if (element.isSubclassOf(IrTree.loop)) {
                println("transformedLoops[", element.visitorParameterName, "] = this")
            }
            if (element.isSubclassOf(IrTree.moduleFragment)) {
                println("this@DeepCopyIrTreeWithSymbols.transformedModule = this")
            }
            for (field in applyFields) {
                when {
                    field.isMutable -> {
                        print(field.name, " = ")
                        copyField(element, field)
                        println()
                    }
                    field is ListField && field.mutability == ListField.Mutability.MutableList -> {
                        print("${element.visitorParameterName}.${field.name}.mapTo(${field.name}) { it")
                        copyValue(field)
                        println(" }")
                    }
                }
            }
            if (element.isSubclassOf(IrTree.memberAccessExpression) && !element.isSubclassOf(IrTree.localDelegatedPropertyReference)) {
                println("copyRemappedTypeArgumentsFrom(", element.visitorParameterName, ")")
                println("transformValueArguments(", element.visitorParameterName, ")")
            }
            if (element.isSubclassOf(IrTree.function)) {
                println("parameters = ${element.visitorParameterName}.parameters.memoryOptimizedMap { it.transform() }")
            }
            if (element.isSubclassOf(IrTree.valueParameter)) {
                println("_kind = ${element.visitorParameterName}._kind")
            }
            if (element.isSubclassOf(IrTree.file)) {
                println("module = transformedModule ?: ${element.visitorParameterName}.module")
            }
            if (element.isSubclassOf(IrTree.moduleFragment)) {
                println("this@DeepCopyIrTreeWithSymbols.transformedModule = null")
            }

            println("processAttributes(", element.visitorParameterName, ")")
        }
    }

    private fun ImportCollectingPrinter.printWithShapeExtraArguments(element: Element) {
        println("typeArgumentsCount = ${element.visitorParameterName}.typeArguments.size,")
        println("hasDispatchReceiver = ${element.visitorParameterName}.targetHasDispatchReceiver,")
        println("hasExtensionReceiver = ${element.visitorParameterName}.targetHasExtensionReceiver,")
        if (!element.isSubclassOf(IrTree.propertyReference)) {
            println("valueArgumentsCount = ${element.visitorParameterName}.valueArgumentsCount,")
            println("contextParameterCount = ${element.visitorParameterName}.targetContextParameterCount,")
        }
    }
}
