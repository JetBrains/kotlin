/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent

internal class DeepCopyIrTreeWithSymbolsPrinter(printer: SmartPrinter, visitorType: ClassRef<*>) :
    TransformerVoidPrinter(printer, visitorType) {

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>
        get() = elementTransformerVoidType

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

    override val optIns: List<ClassRef<*>> = listOf(obsoleteDescriptorBasedApiAnnotation)

    context(ImportCollector)
    override fun printVisitor(elements: List<Element>) {
        printVisitorClass(elements)
    }

    context(ImportCollector)
    override fun SmartPrinter.printAdditionalMethods() {
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
            type = null,
            kind = VariableKind.VAL,
            visibility = Visibility.PRIVATE,
            initializer = "${type<HashMap<*, *>>().withArgs(IrTree.loop, IrTree.loop).render()}()"
        )
        println()
        println()
        printInitBlock()
        println()
        printDeprecatedConstructor()
        println()
        printHelperMethods()
    }

    context(ImportCollector)
    private fun SmartPrinter.printDeprecatedConstructor() {
        val delegatingCall = "(${constructorParameters.joinToString(", ") { it.name }})"
        val unusedParameter = FunctionParameter(
            name = "symbolRenamer",
            type = symbolRenamerType.copy(nullable = true),
            defaultValue = "null",
            annotations = listOf("@Suppress(\"UNUSED_PARAMETER\")")
        )
        printSecondaryConstructorDeclaration(
            parameters = constructorParameters.map { it.functionParameter } + unusedParameter,
            allParametersOnSeparateLines = true,
            deprecation = Deprecated(
                "Use the primary constructor; this one is left for compatibility with Compose",
                ReplaceWith("${visitorType.simpleName}${delegatingCall}"),
            ),
        )
        println(" : this$delegatingCall")
    }

    context(ImportCollector)
    private fun SmartPrinter.printInitBlock() {
        print("init")
        printBlock {
            println("// TODO refactor")
            println("// After removing usages of ${deepCopyTypeRemapperType.simpleName} constructor from compose, the lateinit property `${deepCopyTypeRemapperType.simpleName}.deepCopy`")
            println("// can be refactored to a constructor parameter.")
            print("(this.${typeRemapperParameter.name} as? ${deepCopyTypeRemapperType.simpleName})?.let")
            printBlock {
                println("it.deepCopy = this")
            }
        }
    }

    context(ImportCollector)
    private fun SmartPrinter.printHelperMethods() {
        addImport(ArbitraryImportable("org.jetbrains.kotlin.utils", "memoryOptimizedMap"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.declarations", "copyAttributes"))
        printlnMultiLine(
            @Suppress("IncorrectFormatting")
            """
            protected fun mapDeclarationOrigin(origin: ${declarationOriginType.render()}) = origin
            protected fun mapStatementOrigin(origin: ${statementOriginType.copy(nullable = true).render()}) = origin

            protected open fun <D : ${IrTree.attributeContainer.render()}> D.processAttributes(other: ${IrTree.attributeContainer.copy(nullable = true).render()}): D =
                copyAttributes(other)

            protected inline fun <reified T : ${IrTree.rootElement.render()}> T.transform() =
                transform(this@${visitorType.render()}, null) as T

            protected inline fun <reified T : ${IrTree.rootElement.render()}> List<T>.transform() =
                memoryOptimizedMap { it.transform() }

            protected inline fun <reified T : ${IrTree.rootElement.render()}> List<T>.transformTo(destination: MutableList<T>) =
                mapTo(destination) { it.transform() }

            protected fun <T : ${IrTree.declarationContainer.render()}> T.transformDeclarationsTo(destination: T) =
                declarations.transformTo(destination.declarations)

            protected fun ${irTypeType.render()}.remapType() = typeRemapper.remapType(this)
        
            protected fun ${IrTree.mutableAnnotationContainer.render()}.transformAnnotations(declaration: ${irAnnotationContainerType.render()}) {
                annotations = declaration.annotations.transform()
            }

            private fun ${IrTree.memberAccessExpression.withStarArgs().render()}.copyRemappedTypeArgumentsFrom(other: ${IrTree.memberAccessExpression.withStarArgs().render()}) {
                assert(typeArgumentsCount == other.typeArgumentsCount) {
                    "Mismatching type arguments: ${'$'}typeArgumentsCount vs ${'$'}{other.typeArgumentsCount} "
                }
                for (i in 0 until typeArgumentsCount) {
                    putTypeArgument(i, other.getTypeArgument(i)?.remapType())
                }
            }

            private fun shallowCopyCall(expression: ${IrTree.call.render()}): ${IrTree.call.render()} {
                val newCallee = symbolRemapper.getReferencedSimpleFunction(expression.symbol)
                return ${IrTree.call.implementations.single().render()}(
                    expression.startOffset, expression.endOffset,
                    expression.type.remapType(),
                    newCallee,
                    expression.typeArgumentsCount,
                    expression.valueArgumentsCount,
                    mapStatementOrigin(expression.origin),
                    symbolRemapper.getReferencedClassOrNull(expression.superQualifierSymbol)
                ).apply {
                    copyRemappedTypeArgumentsFrom(expression)
                }.processAttributes(expression)
            }

            private fun <T : ${IrTree.memberAccessExpression.withStarArgs().render()}> T.transformReceiverArguments(original: T): T =
                apply {
                    dispatchReceiver = original.dispatchReceiver?.transform()
                    extensionReceiver = original.extensionReceiver?.transform()
                }

            private fun <T : ${IrTree.memberAccessExpression.withStarArgs().render()}> T.transformValueArguments(original: T) {
                transformReceiverArguments(original)
                for (i in 0 until original.valueArgumentsCount) {
                    putValueArgument(i, original.getValueArgument(i)?.transform())
                }
            }

            private fun getTransformedLoop(irLoop: ${IrTree.loop.render()}): ${IrTree.loop.render()} =
                transformedLoops.getOrDefault(irLoop, irLoop)

            private fun ${symbolRemapperType.render()}.getReferencedReturnTarget(returnTarget: ${returnTargetSymbolType.render()}): ${returnTargetSymbolType.render()} =
                when (returnTarget) {
                    is ${functionSymbolType.render()} -> getReferencedFunction(returnTarget)
                    is ${returnableBlockSymbolType.render()} -> getReferencedReturnableBlock(returnTarget)
                }

            private fun copyTypeParameter(declaration: ${IrTree.typeParameter.render()}): ${IrTree.typeParameter.render()} =
                declaration.factory.createTypeParameter(
                    startOffset = declaration.startOffset,
                    endOffset = declaration.endOffset,
                    origin = mapDeclarationOrigin(declaration.origin),
                    name = declaration.name,
                    symbol = symbolRemapper.getDeclaredTypeParameter(declaration.symbol),
                    variance = declaration.variance,
                    index = declaration.index,
                    isReified = declaration.isReified,
                ).apply {
                    transformAnnotations(declaration)
                }

            protected fun ${IrTree.typeParametersContainer.render()}.copyTypeParametersFrom(other: ${IrTree.typeParametersContainer.render()}) {
                this.typeParameters = other.typeParameters.memoryOptimizedMap {
                    copyTypeParameter(it)
                }

                typeRemapper.withinScope(this) {
                    for ((thisTypeParameter, otherTypeParameter) in this.typeParameters.zip(other.typeParameters)) {
                        thisTypeParameter.superTypes = otherTypeParameter.superTypes.memoryOptimizedMap {
                            typeRemapper.remapType(it)
                        }
                    }
                }
            }

            private fun <T : ${IrTree.function.render()}> T.transformFunctionChildren(declaration: T): T =
                apply {
                    transformAnnotations(declaration)
                    copyTypeParametersFrom(declaration)
                    typeRemapper.withinScope(this) {
                        dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                        extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                        returnType = typeRemapper.remapType(declaration.returnType)
                        valueParameters = declaration.valueParameters.transform()
                        body = declaration.body?.transform()
                    }
                }
            """
        )
    }

    context(ImportCollector)
    override fun printMethodsForElement(element: Element) {
        // FIXME: Use a more elegant solution
        if (element == IrTree.functionWithLateBinding || element == IrTree.propertyWithLateBinding) return

        printer.run {
            if (element.isRootElement) {
                println()
                printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)
                println(" =")
                withIndent {
                    println("throw IllegalArgumentException(\"Unsupported element type: \$${element.visitorParameterName}\")")
                }
                return
            }

            if (element.implementations.isEmpty()) return
            println()
            printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)
            println(" =")
            withIndent {
                val constructorArguments: List<Pair<String, Field>> =
                    if (element.generateIrFactoryMethod) {
                        val factoryMethod = FactoryMethod(element)
                        if (element.category == Element.Category.Declaration) {
                            print(element.visitorParameterName, ".factory")
                        } else {
                            print(irFactoryImplType.render())
                        }
                        print(".", factoryMethod.name)
                        factoryMethod.parametersAndFields.mapNotNull { (parameter, field) -> field?.let { parameter.name to it } }
                    } else {
                        val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                        print(implementation.render())
                        implementation.fieldsInConstructor.map { it.name to it }
                    }

                val fieldsInApply = if (element.generateIrFactoryMethod) {
                    element.allFields - constructorArguments.map { it.second }.toSet()
                } else {
                    val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                    implementation.fieldsInBody
                }

                println("(")
                withIndent {
                    for ((parameterName, field) in constructorArguments) {
                        print(parameterName, " = ")
                        copyField(element, field)
                        println(",")
                    }
                }
                print(").apply")
                printBlock {
                    if (element.isSubclassOf(irAnnotationContainerType)) {
                        println("transformAnnotations(", element.visitorParameterName, ")")
                    }
                    if (element.isSubclassOf(IrTree.typeParametersContainer)) {
                        println("copyTypeParametersFrom(", element.visitorParameterName, ")")
                    }
                    for (field in fieldsInApply) {
                        if (constructorArguments.any { it.second == field }) continue
                        if (field.implementationDefaultStrategy?.withGetter == true) continue
                        if (field.name in setOf("parent", "factory", "descriptor")) continue
                        if (field in IrTree.typeParametersContainer.fields) continue
                        if (field in IrTree.attributeContainer.fields) continue
                        if (field in IrTree.metadataSourceOwner.fields) continue
                        if (field in IrTree.mutableAnnotationContainer.fields) continue
                        if (field is ListField) {
                            when (field.mutability) {
                                ListField.Mutability.Var -> {
                                    print(field.name, " = ")
                                    print(element.visitorParameterName, ".", field.name, field.call(), "memoryOptimizedMap")
                                    printBlock {
                                        copyValue(field.baseType, "it")
                                        println()
                                    }
                                }
                                ListField.Mutability.MutableList -> {}
                                ListField.Mutability.Array -> {}
                            }

                        } else {
                            print(field.name, " = ")
                            copyField(element, field)
                            println()
                        }
                    }
                    if (element.isSubclassOf(IrTree.declarationContainer)) {
                        println(element.visitorParameterName, ".transformDeclarationsTo(this)")
                    }
                    if (element.isSubclassOf(IrTree.attributeContainer)) {
                        println("processAttributes(", element.visitorParameterName, ")")
                    }
                }
            }
        }
    }

    private fun Field.hasClass(ref: ClassOrElementRef): Boolean = (typeRef as? ClassOrElementRef)?.isSameClassAs(ref) ?: false

    context(ImportCollector)
    private fun SmartPrinter.copyValue(typeRef: TypeRef, vararg valueArgs: Any?) {
        val safeCall = if (typeRef.nullable) "?." else "."
        when {
            typeRef !is ClassOrElementRef -> {
                print(*valueArgs)
            }
            typeRef is ElementOrRef<*> -> {
                print(*valueArgs, safeCall, "transform()")
            }
            typeRef.isSameClassAs(declarationOriginType) -> {
                print("mapDeclarationOrigin(", *valueArgs, ")")
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

    context(ImportCollector)
    private fun SmartPrinter.copyField(element: Element, field: Field) {
        copyValue(field.typeRef, element.visitorParameterName, ".", field.name)
    }
}