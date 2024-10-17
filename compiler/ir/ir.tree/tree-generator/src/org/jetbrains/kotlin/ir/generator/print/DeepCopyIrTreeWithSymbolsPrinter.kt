/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.descriptors.ValueClassRepresentation
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.ir.generator.IrTree
import org.jetbrains.kotlin.ir.generator.IrTree.functionWithLateBinding
import org.jetbrains.kotlin.ir.generator.IrTree.propertyWithLateBinding
import org.jetbrains.kotlin.ir.generator.IrTree.suspendableExpression
import org.jetbrains.kotlin.ir.generator.IrTree.suspensionPoint
import org.jetbrains.kotlin.ir.generator.deepCopyTypeRemapperType
import org.jetbrains.kotlin.ir.generator.elementTransformerVoidType
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Implementation
import org.jetbrains.kotlin.ir.generator.model.ListField
import org.jetbrains.kotlin.ir.generator.obsoleteDescriptorBasedApiAnnotation
import org.jetbrains.kotlin.ir.generator.symbolRemapperType
import org.jetbrains.kotlin.ir.generator.typeRemapperType
import org.jetbrains.kotlin.utils.withIndent

internal class DeepCopyIrTreeWithSymbolsPrinter(
    printer: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(printer) {
    override val visitorTypeParameters: List<TypeVariable>
        get() = emptyList()

    // FIXME: not used
    override val visitorDataType: TypeRef
        get() = StandardTypes.nothing.copy(nullable = true)

    override fun visitMethodReturnType(element: Element): TypeRef = element

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(elementTransformerVoidType)

    override val optIns: List<ClassRef<*>> = listOf(obsoleteDescriptorBasedApiAnnotation)

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
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.types", "IrType"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir", "IrStatement"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir", "IrImplementationDetail"))

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
        println()
        printInitBlock()
        println()
        printUtils()
    }

    private fun ImportCollectingPrinter.printInitBlock() {
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

    private fun ImportCollectingPrinter.printUtils() {
        printlnMultiLine(
            """
            // TODO: remove these two functions and others that can be safely removed
            protected fun mapDeclarationOrigin(origin: IrDeclarationOrigin) = origin
            protected fun mapStatementOrigin(origin: IrStatementOrigin?) = origin
        
            protected open fun <D : IrAttributeContainer> D.processAttributes(other: IrAttributeContainer?): D =
                copyAttributes(other)
        
            protected inline fun <reified T : IrElement> T.transform() =
                transform(this@DeepCopyIrTreeWithSymbols, null) as T
        
            protected inline fun <reified T : IrElement> List<T>.transform() =
                memoryOptimizedMap { it.transform() }
        
            protected inline fun <reified T : IrElement> List<T>.transformTo(destination: MutableList<T>) =
                mapTo(destination) { it.transform() }
        
            protected fun <T : IrDeclarationContainer> T.transformDeclarationsTo(destination: T) =
                declarations.transformTo(destination.declarations)
        
            protected fun IrType.remapType() = typeRemapper.remapType(this)
            
            override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement =
                throw IllegalArgumentException("Unsupported declaration type: ${'$'}declaration")
            
            private fun <T : IrFunction> T.transformFunctionChildren(declaration: T): T =
                apply {
                    typeRemapper.withinScope(this) {
                        dispatchReceiverParameter = declaration.dispatchReceiverParameter?.transform()
                        extensionReceiverParameter = declaration.extensionReceiverParameter?.transform()
                        returnType = typeRemapper.remapType(declaration.returnType)
                        valueParameters = declaration.valueParameters.transform()
                        body = declaration.body?.transform()
                    }
                }
        
            protected fun IrMutableAnnotationContainer.transformAnnotations(declaration: IrAnnotationContainer) {
                annotations = declaration.annotations.transform()
            }
        
            private fun copyTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
                declaration.factory.createTypeParameter(
                    startOffset = declaration.startOffset,
                    endOffset = declaration.endOffset,
                    origin = declaration.origin,
                    name = declaration.name,
                    symbol = symbolRemapper.getDeclaredTypeParameter(declaration.symbol),
                    variance = declaration.variance,
                    index = declaration.index,
                    isReified = declaration.isReified,
                ).apply {
                    annotations = declaration.annotations.memoryOptimizedMap { it.transform() }
                }
        
            protected fun IrTypeParametersContainer.copyTypeParametersFrom(other: IrTypeParametersContainer) {
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
        
            override fun visitBody(body: IrBody): IrBody =
                throw IllegalArgumentException("Unsupported body type: ${'$'}body")
        
            override fun visitExpression(expression: IrExpression): IrExpression =
                throw IllegalArgumentException("Unsupported expression type: ${'$'}expression")
        
            protected fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression<*>) {
                assert(typeArgumentsCount == other.typeArgumentsCount) {
                    "Mismatching type arguments: ${'$'}typeArgumentsCount vs ${'$'}{other.typeArgumentsCount} "
                }
                for (i in 0 until typeArgumentsCount) {
                    putTypeArgument(i, other.getTypeArgument(i)?.remapType())
                }
            }
        
            protected fun <T : IrMemberAccessExpression<*>> T.transformReceiverArguments(original: T): T =
                apply {
                    dispatchReceiver = original.dispatchReceiver?.transform()
                    extensionReceiver = original.extensionReceiver?.transform()
                }
        
            protected fun <T : IrMemberAccessExpression<*>> T.transformValueArguments(original: T) {
                transformReceiverArguments(original)
                for (i in 0 until original.valueArgumentsCount) {
                    putValueArgument(i, original.getValueArgument(i)?.transform())
                }
            }
        
            private val transformedLoops = HashMap<IrLoop, IrLoop>()
            """
        )
    }

    override fun printMethodsForElement(element: Element) {
        if (element in setOf(functionWithLateBinding, propertyWithLateBinding, suspendableExpression, suspensionPoint)) return

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

            if (element.isSubclassOf(IrTree.expressionBody)) {
                // TODO: `printVisitMethodDeclaration` 3 times, can be fixed later
                println()
                printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)
                printBlock {
                    printlnMultiLine(
                        """
                        val expression = body.expression.transform()
                        return IrExpressionBodyImpl(
                            startOffset = expression.startOffset,
                            endOffset = expression.endOffset,
                            expression = expression,
                            constructorIndicator = null,
                        )
                        """
                    )
                }
                return
            }

            if (element.isSubclassOf(IrTree.blockBody)) {
                println()
                printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)
                println(" =")
                withIndent {
                    // TODO: maybe some parts can be automatic
                    printlnMultiLine(
                        """
                        IrBlockBodyImpl(
                            startOffset = body.startOffset,
                            endOffset = body.endOffset,
                            constructorIndicator = null
                        ).apply {
                            statements.addAll(body.statements.memoryOptimizedMap { it.transform() })
                        }
                        """
                    )
                }
                return
            }

            if (element.implementations.isEmpty()) return
            println()
            if (element.isSubclassOf(IrTree.declaration)) {
                println("@OptIn(IrImplementationDetail::class)")
            }
            printVisitMethodDeclaration(element, hasDataParameter = false, override = true, returnType = element)
            println(" =")
            withIndent {
                val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                print(implementation.render())
                if (isWithShape(element)) {
                    print("WithShape")
                }
                val constructorArguments: List<Pair<String, Field>> =
                    implementation.constructorArguments()
                println("(")
                withIndent {
                    for ((parameterName, field) in constructorArguments) {
                        print(parameterName, " = ")
                        copyField(element, field)
                        println(",")
                    }
                    if (element.isSubclassOf(IrTree.errorDeclaration)) {
                        println("origin = IrDeclarationOrigin.DEFINED,")
                    }
                    if (isWithShape(element)) {
                        printlnMultiLine(
                            """
                            typeArgumentsCount = ${element.visitorParameterName}.typeArgumentsCount,
                            hasDispatchReceiver = ${element.visitorParameterName}.targetHasDispatchReceiver,
                            hasExtensionReceiver = ${element.visitorParameterName}.targetHasExtensionReceiver,
                            """
                        )
                        if (!element.isSubclassOf(IrTree.propertyReference)) {
                            printlnMultiLine(
                                """
                                valueArgumentsCount = ${element.visitorParameterName}.valueArgumentsCount,
                                contextParameterCount = ${element.visitorParameterName}.targetContextParameterCount,
                                """
                            )
                        }
                    }
                }
                // TODO: check if the set of included is actually smaller
                val excludedApplyFields = setOf(
                    "originalBeforeInline",
                    "attributeOwnerId",
                    "startOffset",
                    "endOffset",
                    "metadata",
                    "typeParameters",
                    "dispatchReceiver",
                    "dispatchReceiverParameter",
                    "extensionReceiver",
                    "extensionReceiverParameter",
                    "valueParameters",
                    "returnType",
                    "origin",
                    "type",
                    "source",
                    "correspondingPropertySymbol",
                    "module",
                )
                val excludedApplyFieldsFor = mapOf(
                    "body" to setOf("Constructor", "SimpleFunction"),
                    "contextReceiverParametersCount" to setOf("Constructor"),
                    "overriddenSymbols" to setOf("SimpleFunction")
                )
                val fieldsInApply =
                    implementation.allFields.filter { it.isMutable && it.name !in excludedApplyFields }.filter {
                        val excludedFields = excludedApplyFieldsFor[it.name]
                        excludedFields == null || element.name !in excludedFields
                    } - implementation.constructorArguments().map { it.second }
                printApply(element, fieldsInApply)
            }
        }
    }

    private fun ImportCollectingPrinter.copyField(element: Element, field: Field) {
        if (field is ListField) {
            print(element.visitorParameterName, ".", field.name, field.call(), "memoryOptimizedMap")
            print(" { ")
            copyValue(element, field, "it")
            print(" }")
        } else {
            copyValue(element, field, element.visitorParameterName, ".", field.name)
            if (element.isSubclassOf(IrTree.property) && field.name in setOf("backingField", "getter", "setter")) {
                print("?.also { it.correspondingPropertySymbol = symbol }")
            }
        }
    }

    // TODO: fix signature (element)
    private fun ImportCollectingPrinter.copyValue(element: Element, field: Field, vararg valueArgs: Any?) {
        val typeRef = if (field is ListField) {
            field.baseType
        } else {
            field.typeRef
        }
        val safeCall = if (typeRef.nullable) "?." else "."
        if (element.name == "When") {
            print("")
        }
        when {
            typeRef !is ClassOrElementRef -> {
                print(*valueArgs)
            }
            typeRef.isSymbol() -> {
                when {
                    element.isSubclassOf(IrTree.inlinedFunctionBlock) -> {
                        print(*valueArgs)
                    }
                    typeRef.nullable -> {
                        print(
                            *valueArgs,
                            "?.let(",
                            symbolRemapperParameter.name,
                            "::",
                            element.getIrFunctionForSymbol(field),
                            typeRef.getIrSymbolName(),
                            ")"
                        )
                    }
                    else -> {
                        print(
                            symbolRemapperParameter.name,
                            ".",
                            element.getIrFunctionForSymbol(field),
                            typeRef.getIrSymbolName(),
                            "(",
                            *valueArgs,
                            ")"
                        )
                    }
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

    private fun ClassOrElementRef.isSymbol(): Boolean =
        packageName == "org.jetbrains.kotlin.ir.symbols.impl" || packageName == "org.jetbrains.kotlin.ir.symbols"

    private fun Element.getIrFunctionForSymbol(field: Field): String = when {
        // TODO: do it better
        field.name != "symbol" -> "getReferenced"
        isSubclassOf(IrTree.declaration) || isSubclassOf(IrTree.packageFragment) || isSubclassOf(IrTree.returnableBlock) -> "getDeclared"
        isSubclassOf(IrTree.expression) -> "getReferenced"
        // TODO: better message
        else -> throw IllegalArgumentException("Unsupported element type: $this")
    }

    fun ClassOrElementRef.getIrSymbolName(): String =
        typeName.replace("Ir", "").replace("Symbol", "")


    fun ClassOrElementRef.isSameClassAs(other: ClassOrElementRef): Boolean =
        packageName == other.packageName && typeName == other.typeName

    private fun Implementation.constructorArguments(): List<Pair<String, Field>> {
        val alwaysExcludedConstructorFields = setOf("valueArguments", "typeArguments", "irBuiltins")
        val excludedConstructorFields = mapOf(
            "origin" to setOf("EnumConstructorCall", "DelegatingConstructorCall", "ErrorDeclaration"),
            "type" to setOf("ConstantPrimitive"),
            "source" to setOf("ConstructorCall"),
        )
        // TODO: probably better with subtypes
        val includedBodyFields = mapOf(
            "branches" to setOf("When"),
            "result" to setOf("Catch"),
            "elements" to setOf("Vararg", "ConstantArray"),
            "tryResult" to setOf("Try"),
            "catches" to setOf("Try"),
            "finallyExpression" to setOf("Try"),
            "arguments" to setOf("StringConcatenation"),
            "receiver" to setOf("SetField", "GetField"),
            "value" to setOf("SetField"),
            "statements" to setOf("BlockBody", "InlinedFunctionBlock", "ReturnableBlock", "Composite", "Block"),
            "valueArguments" to setOf("ConstantObject"),
            "typeArguments" to setOf("ConstantObject"),
        )
        val filteredConstructorFields = fieldsInConstructor.filter {
            it.name !in alwaysExcludedConstructorFields &&
                    excludedConstructorFields[it.name]?.contains(element.name) != true
        }
        val filteredBodyFields = fieldsInBody.filter {
            val includedElements = includedBodyFields[it.name]
            includedElements != null && element.name in includedElements
        }
        val allFields = filteredConstructorFields + filteredBodyFields
        return allFields.map { it.name to it }
    }

    private fun isWithShape(element: Element): Boolean {
        val elementsWithShape = setOf(
            IrTree.functionAccessExpression,
            IrTree.functionReference,
            IrTree.propertyReference,
        )
        return elementsWithShape.any { element.isSubclassOf(it) }
    }

    private fun ImportCollectingPrinter.printApply(element: Element, applyFields: List<Field>) {
        if (
            element.isSubclassOf(IrTree.mutableAnnotationContainer) ||
            element.isSubclassOf(IrTree.typeParametersContainer) ||
            element.isSubclassOf(IrTree.declarationContainer) ||
            element.isSubclassOf(IrTree.attributeContainer) ||
            element.isSubclassOf(IrTree.moduleFragment) ||
            applyFields.isNotEmpty()
        ) {
            print(").apply")
            printBlock {
                if (element.isSubclassOf(IrTree.declaration) && !element.isSubclassOf(IrTree.variable)) {
                    println("with(factory) { declarationCreated() }")
                }
                if (element.isSubclassOf(IrTree.script)) {
                    printScriptApply()
                    return@printBlock
                }
                if (element.isSubclassOf(IrTree.loop)) {
                    println("transformedLoops[", element.visitorParameterName, "] = this")
                }
                applyFields.forEach { field ->
                    print(field.name, " = ")
                    copyField(element, field)
                    println()
                }
                // TODO: data structure for these if-conditions
                if (element.isSubclassOf(IrTree.typeParametersContainer)) {
                    println("copyTypeParametersFrom(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.declarationContainer)) {
                    println(element.visitorParameterName, ".transformDeclarationsTo(this)")
                }
                if (element.isSubclassOf(IrTree.attributeContainer)) {
                    println("processAttributes(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.memberAccessExpression) && !element.isSubclassOf(IrTree.localDelegatedPropertyReference)) {
                    println("copyRemappedTypeArgumentsFrom(", element.visitorParameterName, ")")
                    println("transformValueArguments(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.function)) {
                    println("transformFunctionChildren(", element.visitorParameterName, ")")
                }
                if (element.isSubclassOf(IrTree.simpleFunction)) {
                    addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.symbols", "IrSimpleFunctionSymbol"))
                    printlnMultiLine(
                        """
                        overriddenSymbols = declaration.overriddenSymbols.memoryOptimizedMap {
                            symbolRemapper.getReferencedFunction(it) as IrSimpleFunctionSymbol
                        }
                        """
                    )
                }
                if (element.isSubclassOf(IrTree.errorCallExpression) || element.isSubclassOf(IrTree.dynamicOperatorExpression)) {
                    println("expression.arguments.transformTo(arguments)")
                }
                if (element.isSubclassOf(IrTree.file)) {
                    println("module = transformedModule ?: declaration.module")
                }
                if (element.isSubclassOf(IrTree.moduleFragment)) {
                    printlnMultiLine(
                        """
                        this@DeepCopyIrTreeWithSymbols.transformedModule = this
                        files += declaration.files.transform()
                        this@DeepCopyIrTreeWithSymbols.transformedModule = null
                        """
                    )
                }
            }
        } else {
            println(")")
        }
    }

    private fun ImportCollectingPrinter.printScriptApply() {
        printlnMultiLine(
            """
            thisReceiver = declaration.thisReceiver?.transform()
            declaration.statements.mapTo(statements) { it.transform() }
            importedScripts = declaration.importedScripts
            resultProperty = declaration.resultProperty
            earlierScripts = declaration.earlierScripts
            earlierScriptsParameter = declaration.earlierScriptsParameter
            explicitCallParameters = declaration.explicitCallParameters.memoryOptimizedMap { it.transform() }
            implicitReceiversParameters = declaration.implicitReceiversParameters.memoryOptimizedMap { it.transform() }
            providedPropertiesParameters = declaration.providedPropertiesParameters.memoryOptimizedMap { it.transform() }
            """
        )
    }
}