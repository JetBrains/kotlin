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
import org.jetbrains.kotlin.ir.generator.declarationOriginType
import org.jetbrains.kotlin.ir.generator.deepCopyTypeRemapperType
import org.jetbrains.kotlin.ir.generator.elementTransformerVoidType
import org.jetbrains.kotlin.ir.generator.irSimpleTypeType
import org.jetbrains.kotlin.ir.generator.irTypeType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
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
        // TODO: remove mapDeclarationOrigin and mapStatementOrigin
        // TODO: check if import still useful
        addImport(ArbitraryImportable("org.jetbrains.kotlin.utils", "memoryOptimizedMap"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.types", "IrType"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir", "IrStatement"))
        addImport(ArbitraryImportable("org.jetbrains.kotlin.ir.expressions.impl", "*"))

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
        
            protected fun IrMutableAnnotationContainer.transformAnnotations(declaration: IrAnnotationContainer) {
                annotations = declaration.annotations.transform()
            }
        
            private fun copyTypeParameter(declaration: IrTypeParameter): IrTypeParameter =
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
        
            private fun IrConst.shallowCopyConst() = IrConstImpl(startOffset, endOffset, type.remapType(), kind, value)
        
            protected fun IrMemberAccessExpression<*>.copyRemappedTypeArgumentsFrom(other: IrMemberAccessExpression<*>) {
                assert(typeArgumentsCount == other.typeArgumentsCount) {
                    "Mismatching type arguments: ${'$'}typeArgumentsCount vs ${'$'}{other.typeArgumentsCount} "
                }
                for (i in 0 until typeArgumentsCount) {
                    putTypeArgument(i, other.getTypeArgument(i)?.remapType())
                }
            }
        
            private fun shallowCopyCall(expression: IrCall): IrCall {
                val newCallee = symbolRemapper.getReferencedSimpleFunction(expression.symbol)
                return IrCallImplWithShape(
                    startOffset = expression.startOffset,
                    endOffset = expression.endOffset,
                    type = expression.type.remapType(),
                    symbol = newCallee,
                    typeArgumentsCount = expression.typeArgumentsCount,
                    valueArgumentsCount = expression.valueArgumentsCount,
                    contextParameterCount = expression.targetContextParameterCount,
                    hasDispatchReceiver = expression.targetHasDispatchReceiver,
                    hasExtensionReceiver = expression.targetHasExtensionReceiver,
                    origin = mapStatementOrigin(expression.origin),
                    superQualifierSymbol = expression.superQualifierSymbol?.let(symbolRemapper::getReferencedClass)
                ).apply {
                    copyRemappedTypeArgumentsFrom(expression)
                }.processAttributes(expression)
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
        
            private fun getTransformedLoop(irLoop: IrLoop): IrLoop =
                transformedLoops.getOrDefault(irLoop, irLoop)
            """
        )
    }

    override fun printMethodsForElement(element: Element) {
        // FIXME: Use a more elegant solution
        if (element == functionWithLateBinding || element == propertyWithLateBinding) return

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
                val implementation = element.implementations.singleOrNull() ?: error("Ambiguous implementation")
                print(implementation.render())
                val constructorArguments: List<Pair<String, Field>> = implementation.fieldsInConstructor.map { it.name to it }
                val fieldsInApply = implementation.fieldsInBody

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
                    if (element.isSubclassOf(IrTree.mutableAnnotationContainer)) {
                        println("transformAnnotations(", element.visitorParameterName, ")")
                    }
                    if (element.isSubclassOf(IrTree.typeParametersContainer)) {
                        println("copyTypeParametersFrom(", element.visitorParameterName, ")")
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

    private fun ImportCollectingPrinter.copyField(element: Element, field: Field) {
        copyValue(field.typeRef, element.visitorParameterName, ".", field.name)
    }

    private fun ImportCollectingPrinter.copyValue(typeRef: TypeRef, vararg valueArgs: Any?) {
        val safeCall = if (typeRef.nullable) "?." else "."
        when {
            typeRef !is ClassOrElementRef -> {
                print(*valueArgs)
            }
            typeRef.isSymbol() -> {
                if (typeRef.nullable) {
                    print(*valueArgs, "?.let(", symbolRemapperParameter.name, "::getReferenced", typeRef.getIrSymbolName(), ")")
                } else {
                    print(symbolRemapperParameter.name, ".getReferenced", typeRef.getIrSymbolName(), "(", *valueArgs, ")")
                }
            }
            typeRef is ElementOrRef<*> -> {
                print(*valueArgs, safeCall, "transform()")
            }
            // TODO: remove
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

    fun ClassOrElementRef.isSymbol(): Boolean =
        packageName == "org.jetbrains.kotlin.ir.symbols.impl" || packageName == "org.jetbrains.kotlin.ir.symbols"

    fun ClassOrElementRef.getIrSymbolName(): String =
        typeName.replace("Ir", "").replace("Symbol", "")


    fun ClassOrElementRef.isSameClassAs(other: ClassOrElementRef): Boolean =
        packageName == other.packageName && typeName == other.typeName
}