/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenFunctions
import org.jetbrains.kotlin.backend.konan.descriptors.target
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class ExternalModulesDFG(val allTypes: List<DataFlowIR.Type.Declared>,
                                  val publicTypes: Map<Long, DataFlowIR.Type.Public>,
                                  val publicFunctions: Map<Long, DataFlowIR.FunctionSymbol.Public>,
                                  val functionDFGs: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>)

private fun IrClass.getOverridingOf(function: IrFunction) = (function as? IrSimpleFunction)?.let {
    it.allOverriddenFunctions.atMostOne { it.parent == this }
}

private fun IrTypeOperator.isCast() =
        this == IrTypeOperator.CAST || this == IrTypeOperator.IMPLICIT_CAST || this == IrTypeOperator.SAFE_CAST

private fun IrTypeOperator.callsInstanceOf() =
        this == IrTypeOperator.CAST || this == IrTypeOperator.SAFE_CAST
                || this == IrTypeOperator.INSTANCEOF || this == IrTypeOperator.NOT_INSTANCEOF

private class VariableValues {
    data class Variable(val loop: IrLoop?, val values: MutableSet<IrExpression>)

    val elementData = HashMap<IrVariable, Variable>()

    fun addEmpty(variable: IrVariable, loop: IrLoop?) {
        elementData[variable] = Variable(loop, mutableSetOf())
    }

    fun add(variable: IrVariable, element: IrExpression) =
            elementData[variable]?.values?.add(element)

    private fun add(variable: IrVariable, elements: Set<IrExpression>) =
            elementData[variable]?.values?.addAll(elements)

    fun computeClosure() {
        elementData.forEach { (key, _) ->
            add(key, computeValueClosure(key))
        }
    }

    // Computes closure of all possible values for given variable.
    private fun computeValueClosure(value: IrVariable): Set<IrExpression> {
        val result = mutableSetOf<IrExpression>()
        val seen = mutableSetOf<IrVariable>()
        dfs(value, seen, result)
        return result
    }

    private fun dfs(value: IrVariable, seen: MutableSet<IrVariable>, result: MutableSet<IrExpression>) {
        seen += value
        val elements = elementData[value]?.values ?: return
        for (element in elements) {
            if (element !is IrGetValue)
                result += element
            else {
                val declaration = element.symbol.owner
                if (declaration is IrVariable && !seen.contains(declaration))
                    dfs(declaration, seen, result)
            }
        }
    }
}

private class ExpressionValuesExtractor(val context: Context,
                                        val returnableBlockValues: Map<IrReturnableBlock, List<IrExpression>>,
                                        val suspendableExpressionValues: Map<IrSuspendableExpression, List<IrSuspensionPoint>>) {

    val unit = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.unitType, context.ir.symbols.unit)

    val nothing = IrGetObjectValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            context.irBuiltIns.nothingType, context.ir.symbols.nothing)

    fun forEachValue(expression: IrExpression, block: (IrExpression) -> Unit) {
        when (expression) {
            is IrReturnableBlock -> returnableBlockValues[expression]!!.forEach { forEachValue(it, block) }

            is IrSuspendableExpression ->
                (suspendableExpressionValues[expression]!! + expression.result).forEach { forEachValue(it, block) }

            is IrSuspensionPoint -> {
                forEachValue(expression.result, block)
                forEachValue(expression.resumeResult, block)
            }

            is IrContainerExpression -> {
                if (expression.statements.isNotEmpty())
                    forEachValue(
                            expression = (expression.statements.last() as? IrExpression) ?: unit,
                            block      = block
                    )
            }

            is IrWhen -> expression.branches.forEach { forEachValue(it.result, block) }

            is IrMemberAccessExpression<*> -> block(expression)

            is IrGetValue -> block(expression)

            is IrGetField -> block(expression)

            is IrVararg -> /* Sometimes, we keep vararg till codegen phase (for constant arrays). */
                block(expression)

            is IrConst<*> -> block(expression)

            is IrTypeOperatorCall -> {
                if (!expression.operator.isCast())
                    block(expression)
                else { // Propagate cast to sub-values.
                    forEachValue(expression.argument) { value ->
                        with(expression) {
                            block(IrTypeOperatorCallImpl(startOffset, endOffset, type, operator, typeOperand, value))
                        }
                    }
                }
            }

            is IrTry -> {
                forEachValue(expression.tryResult, block)
                expression.catches.forEach { forEachValue(it.result, block) }
            }

            is IrGetObjectValue -> block(expression)

            is IrFunctionReference -> block(expression)

            is IrSetField -> block(expression)

            else -> when {
                expression.type.isUnit() -> unit
                expression.type.isNothing() -> nothing
                else -> TODO(ir2stringWhole(expression))
            }
        }
    }
}

internal class ModuleDFG(val functions: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>,
                         val symbolTable: DataFlowIR.SymbolTable)

internal class ModuleDFGBuilder(val context: Context, val irModule: IrModuleFragment) {

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    private val TAKE_NAMES = true // Take fqNames for all functions and types (for debug purposes).

    private inline fun takeName(block: () -> String) = if (TAKE_NAMES) block() else null

    private val module = DataFlowIR.Module(irModule.descriptor)
    private val symbolTable = DataFlowIR.SymbolTable(context, irModule, module)

    // Possible values of a returnable block.
    private val returnableBlockValues = mutableMapOf<IrReturnableBlock, MutableList<IrExpression>>()

    // All suspension points within specified suspendable expression.
    private val suspendableExpressionValues = mutableMapOf<IrSuspendableExpression, MutableList<IrSuspensionPoint>>()

    private val expressionValuesExtractor = ExpressionValuesExtractor(context, returnableBlockValues, suspendableExpressionValues)

    fun build(): ModuleDFG {
        val functions = mutableMapOf<DataFlowIR.FunctionSymbol, DataFlowIR.Function>()
        irModule.accept(object : IrElementVisitorVoid {

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitConstructor(declaration: IrConstructor) {
                val body = declaration.body
                assert (body != null || declaration.constructedClass.isNonGeneratedAnnotation()) {
                    "Non-annotation class constructor has empty body"
                }
                DEBUG_OUTPUT(0) {
                    println("Analysing function ${declaration.descriptor}")
                    println("IR: ${ir2stringWhole(declaration)}")
                }
                analyze(declaration, body)
            }

            override fun visitFunction(declaration: IrFunction) {
                val body = declaration.body
                if (body == null) {
                    // External function or intrinsic.
                    symbolTable.mapFunction(declaration)
                } else {
                    DEBUG_OUTPUT(0) {
                        println("Analysing function ${declaration.descriptor}")
                        println("IR: ${ir2stringWhole(declaration)}")
                    }
                    analyze(declaration, body)
                }
            }

            override fun visitField(declaration: IrField) {
                if (declaration.parent is IrFile)
                    declaration.initializer?.let {
                        DEBUG_OUTPUT(0) {
                            println("Analysing global field ${declaration.descriptor}")
                            println("IR: ${ir2stringWhole(declaration)}")
                        }
                        analyze(declaration, IrSetFieldImpl(it.startOffset, it.endOffset, declaration.symbol, null,
                                it.expression, context.irBuiltIns.unitType))
                    }
            }

            private fun analyze(declaration: IrDeclaration, body: IrElement?) {
                // Find all interesting expressions, variables and functions.
                val visitor = ElementFinderVisitor()
                body?.acceptVoid(visitor)

                DEBUG_OUTPUT(0) {
                    println("FIRST PHASE")
                    visitor.variableValues.elementData.forEach { t, u ->
                        println("VAR $t [LOOP ${u.loop}]:")
                        u.values.forEach {
                            println("    ${ir2stringWhole(it)}")
                        }
                    }
                    visitor.expressions.forEach { t ->
                        println("EXP [LOOP ${t.value}] ${ir2stringWhole(t.key)}")
                    }
                }

                // Compute transitive closure of possible values for variables.
                visitor.variableValues.computeClosure()

                DEBUG_OUTPUT(0) {
                    println("SECOND PHASE")
                    visitor.variableValues.elementData.forEach { t, u ->
                        println("VAR $t [LOOP ${u.loop}]:")
                        u.values.forEach {
                            println("    ${ir2stringWhole(it)}")
                        }
                    }
                }

                val function = FunctionDFGBuilder(expressionValuesExtractor, visitor.variableValues,
                        declaration, visitor.expressions, visitor.parentLoops, visitor.returnValues,
                        visitor.thrownValues, visitor.catchParameters).build()

                DEBUG_OUTPUT(0) {
                    function.debugOutput()
                }

                functions.put(function.symbol, function)
            }
        }, data = null)

        DEBUG_OUTPUT(1) {
            println("SYMBOL TABLE:")
            symbolTable.classMap.forEach { irClass, type ->
                println("    DESCRIPTOR: ${irClass.descriptor}")
                println("    TYPE: $type")
                if (type !is DataFlowIR.Type.Declared)
                    return@forEach
                println("        SUPER TYPES:")
                type.superTypes.forEach { println("            $it") }
                println("        VTABLE:")
                type.vtable.forEach { println("            $it") }
                println("        ITABLE:")
                type.itable.forEach { println("            ${it.key} -> ${it.value}") }
            }
        }

        return ModuleDFG(functions, symbolTable)
    }

    private inner class ElementFinderVisitor : IrElementVisitorVoid {
        val expressions = mutableMapOf<IrExpression, IrLoop?>()
        val parentLoops = mutableMapOf<IrLoop, IrLoop?>()
        val variableValues = VariableValues()
        val returnValues = mutableListOf<IrExpression>()
        val thrownValues = mutableListOf<IrExpression>()
        val catchParameters = mutableSetOf<IrVariable>()

        private val suspendableExpressionStack = mutableListOf<IrSuspendableExpression>()
        private val loopStack = mutableListOf<IrLoop>()
        private val currentLoop get() = loopStack.peek()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        private fun assignVariable(variable: IrVariable, value: IrExpression) {
            expressionValuesExtractor.forEachValue(value) {
                variableValues.add(variable, it)
            }
        }

        override fun visitExpression(expression: IrExpression) {
            when (expression) {
                is IrMemberAccessExpression<*>,
                is IrGetField,
                is IrGetObjectValue,
                is IrVararg,
                is IrConst<*>,
                is IrTypeOperatorCall ->
                    expressions += expression to currentLoop
            }

            if (expression is IrCall && expression.symbol == executeImplSymbol) {
                // Producer and job of executeImpl are called externally, we need to reflect this somehow.
                val producerInvocation = IrCallImpl(expression.startOffset, expression.endOffset,
                        executeImplProducerInvoke.returnType,
                        executeImplProducerInvoke.symbol)
                producerInvocation.dispatchReceiver = expression.getValueArgument(2)

                expressions += producerInvocation to currentLoop

                val jobFunctionReference = expression.getValueArgument(3) as? IrFunctionReference
                        ?: error("A function reference expected")
                val jobInvocation = IrCallImpl(expression.startOffset, expression.endOffset,
                        jobFunctionReference.symbol.owner.returnType,
                        jobFunctionReference.symbol as IrSimpleFunctionSymbol)
                jobInvocation.putValueArgument(0, producerInvocation)

                expressions += jobInvocation to currentLoop
            }

            // TODO: A little bit hacky but it is the simplest solution.
            // See ObjC instanceOf code generation for details.
            if (expression is IrTypeOperatorCall && expression.operator.callsInstanceOf()
                    && expression.typeOperand.isObjCObjectType()) {
                val objcObjGetter = IrCallImpl(expression.startOffset, expression.endOffset,
                        objCObjectRawValueGetter.owner.returnType,
                        objCObjectRawValueGetter
                ).apply {
                    extensionReceiver = expression.argument
                }
                expressions += objcObjGetter to currentLoop
            }

            if (expression is IrReturnableBlock) {
                returnableBlockValues.put(expression, mutableListOf())
            }
            if (expression is IrSuspendableExpression) {
                suspendableExpressionStack.push(expression)
                suspendableExpressionValues.put(expression, mutableListOf())
            }
            if (expression is IrSuspensionPoint)
                suspendableExpressionValues[suspendableExpressionStack.peek()!!]!!.add(expression)
            if (expression is IrLoop) {
                parentLoops[expression] = currentLoop
                loopStack.push(expression)
            }

            super.visitExpression(expression)

            if (expression is IrLoop)
                loopStack.pop()
            if (expression is IrSuspendableExpression)
                suspendableExpressionStack.pop()
        }

        override fun visitSetField(expression: IrSetField) {
            expressions += expression to currentLoop
            super.visitSetField(expression)
        }

        override fun visitReturn(expression: IrReturn) {
            val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
            if (returnableBlock != null) {
                returnableBlockValues[returnableBlock]!!.add(expression.value)
            } else { // Non-local return.
                if (!expression.type.isUnit())
                    returnValues += expression.value
            }
            super.visitReturn(expression)
        }

        override fun visitThrow(expression: IrThrow) {
            thrownValues += expression.value
            super.visitThrow(expression)
        }

        override fun visitCatch(aCatch: IrCatch) {
            catchParameters.add(aCatch.catchParameter)
            super.visitCatch(aCatch)
        }

        override fun visitSetVariable(expression: IrSetVariable) {
            super.visitSetVariable(expression)
            assignVariable(expression.symbol.owner, expression.value)
        }

        override fun visitVariable(declaration: IrVariable) {
            variableValues.addEmpty(declaration, currentLoop)
            super.visitVariable(declaration)
            declaration.initializer?.let { assignVariable(declaration, it) }
        }
    }

    private val symbols = context.ir.symbols

    private val invokeSuspendFunctionSymbol =
            symbols.baseContinuationImpl.owner.declarations
                    .filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "invokeSuspend" }.symbol

    private val getContinuationSymbol = symbols.getContinuation

    private val arrayGetSymbols = symbols.arrayGet.values
    private val arraySetSymbols = symbols.arraySet.values
    private val createUninitializedInstanceSymbol = symbols.createUninitializedInstance
    private val initInstanceSymbol = symbols.initInstance
    private val executeImplSymbol = symbols.executeImpl
    private val executeImplProducerClass = symbols.functionN(0).owner
    private val executeImplProducerInvoke = executeImplProducerClass.simpleFunctions()
            .single { it.name == OperatorNameConventions.INVOKE }
    private val reinterpret = symbols.reinterpret
    private val objCObjectRawValueGetter = symbols.interopObjCObjectRawValueGetter

    private class Scoped<out T : Any>(val value: T, val scope: DataFlowIR.Node.Scope)

    private inner class FunctionDFGBuilder(val expressionValuesExtractor: ExpressionValuesExtractor,
                                           val variableValues: VariableValues,
                                           val declaration: IrDeclaration,
                                           val expressions: Map<IrExpression, IrLoop?>,
                                           val parentLoops: Map<IrLoop, IrLoop?>,
                                           val returnValues: List<IrExpression>,
                                           val thrownValues: List<IrExpression>,
                                           val catchParameters: Set<IrVariable>) {

        private val rootScope = DataFlowIR.Node.Scope(0, emptyList())
        private val allParameters = (declaration as? IrFunction)?.allParameters ?: emptyList()
        private val templateParameters = allParameters.withIndex().associateBy({ it.value },
                { Scoped(DataFlowIR.Node.Parameter(it.index), rootScope) }
        )

        private val continuationParameter = when {
            declaration !is IrSimpleFunction -> null

            declaration.isSuspend -> Scoped(DataFlowIR.Node.Parameter(allParameters.size), rootScope)

            declaration.overrides(invokeSuspendFunctionSymbol.owner) ->           // <this> is a ContinuationImpl inheritor.
                templateParameters[declaration.dispatchReceiverParameter!!]       // It is its own continuation.

            else -> null
        }

        private fun getContinuation() = continuationParameter
                ?: error("Function ${declaration.descriptor} has no continuation parameter")

        private val nodes = mutableMapOf<IrExpression, Scoped<DataFlowIR.Node>>()
        private val variables = mutableMapOf<IrVariable, Scoped<DataFlowIR.Node.Variable>>()
        private val expressionsScopes = mutableMapOf<IrExpression, DataFlowIR.Node.Scope>()

        fun build(): DataFlowIR.Function {
            val isSuspend = declaration is IrSimpleFunction && declaration.isSuspend

            val scopes = mutableMapOf<IrLoop, DataFlowIR.Node.Scope>()
            fun transformLoop(loop: IrLoop, parentLoop: IrLoop?): DataFlowIR.Node.Scope {
                scopes[loop]?.let { return it }
                val parentScope =
                        if (parentLoop == null)
                            rootScope
                        else transformLoop(parentLoop, parentLoops[parentLoop])
                val scope = DataFlowIR.Node.Scope(parentScope.depth + 1, emptyList())
                parentScope.nodes += scope
                scopes[loop] = scope
                return scope
            }
            parentLoops.forEach { (loop, parentLoop) -> transformLoop(loop, parentLoop) }
            expressions.forEach { (expression, loop) ->
                val scope = if (loop == null) rootScope else scopes[loop]!!
                expressionsScopes[expression] = scope
            }
            expressionsScopes[expressionValuesExtractor.unit] = rootScope
            expressionsScopes[expressionValuesExtractor.nothing] = rootScope

            variableValues.elementData.forEach { (irVariable, variable) ->
                val loop = variable.loop
                val scope = if (loop == null) rootScope else scopes[loop]!!
                val node = DataFlowIR.Node.Variable(
                        values = mutableListOf(),
                        type   = symbolTable.mapType(irVariable.type),
                        kind   = if (catchParameters.contains(irVariable))
                                     DataFlowIR.VariableKind.CatchParameter
                                 else DataFlowIR.VariableKind.Ordinary
                )
                scope.nodes += node
                variables[irVariable] = Scoped(node, scope)
            }

            expressions.forEach { getNode(it.key) }

            val returnNodeType = when (declaration) {
                is IrField -> declaration.type
                is IrFunction -> declaration.returnType
                else -> error(declaration)
            }

            val returnsNode = DataFlowIR.Node.Variable(
                    values = returnValues.map { expressionToEdge(it) },
                    type   = symbolTable.mapType(returnNodeType),
                    kind   = DataFlowIR.VariableKind.Temporary
            )
            val throwsNode = DataFlowIR.Node.Variable(
                    values = thrownValues.map { expressionToEdge(it) },
                    type   = symbolTable.mapClassReferenceType(symbols.throwable.owner),
                    kind   = DataFlowIR.VariableKind.Temporary
            )
            variables.forEach { (irVariable, node) ->
                val values = variableValues.elementData[irVariable]!!.values
                values.forEach { node.value.values += expressionToEdge(it) }
            }

            rootScope.nodes += templateParameters.values.map { it.value }
            rootScope.nodes += returnsNode
            rootScope.nodes += throwsNode
            if (isSuspend)
                rootScope.nodes += continuationParameter!!.value

            return DataFlowIR.Function(
                    symbol = symbolTable.mapFunction(declaration),
                    body   = DataFlowIR.FunctionBody(
                            rootScope, listOf(rootScope) + scopes.values, returnsNode, throwsNode)
            )
        }

        private fun IrType.erasure(): IrType {
            if (this !is IrSimpleType) return this

            val classifier = this.classifier
            return when (classifier) {
                is IrClassSymbol -> this
                is IrTypeParameterSymbol -> {
                    val upperBound = classifier.owner.superTypes.singleOrNull() ?:
                    TODO("${classifier.descriptor} : ${classifier.descriptor.upperBounds}")

                    if (this.hasQuestionMark) {
                        // `T?`
                        upperBound.erasure().makeNullable()
                    } else {
                        upperBound.erasure()
                    }
                }
                else -> TODO(classifier.toString())
            }
        }

        private fun expressionToEdge(expression: IrExpression) = expressionToScopedEdge(expression).value

        private fun expressionToScopedEdge(expression: IrExpression) =
                if (expression is IrTypeOperatorCall && expression.operator.isCast())
                    getNode(expression.argument).let {
                        Scoped(
                                DataFlowIR.Edge(
                                        it.value,
                                        symbolTable.mapClassReferenceType(expression.typeOperand.erasure().getClass()!!)
                                ), it.scope)
                    }
                else {
                    getNode(expression).let {
                        Scoped(
                                DataFlowIR.Edge(it.value, null),
                                it.scope
                        )
                    }
                }

        private fun mapReturnType(actualType: IrType, returnType: IrType): DataFlowIR.Type {
            val returnedInlinedClass = returnType.getInlinedClassNative()
            val actualInlinedClass = actualType.getInlinedClassNative()

            return if (returnedInlinedClass == null) {
                if (actualInlinedClass == null) symbolTable.mapType(actualType) else symbolTable.mapClassReferenceType(actualInlinedClass)
            } else {
                symbolTable.mapType(returnType)
            }
        }

        private fun getNode(expression: IrExpression): Scoped<DataFlowIR.Node> {
            if (expression is IrGetValue) {
                val valueDeclaration = expression.symbol.owner
                if (valueDeclaration is IrValueParameter)
                    return templateParameters[valueDeclaration]!!
                return variables[valueDeclaration]!!
            }
            return nodes.getOrPut(expression) {
                DEBUG_OUTPUT(0) {
                    println("Converting expression")
                    println(ir2stringWhole(expression))
                }
                val values = mutableListOf<IrExpression>()
                val edges = mutableListOf<DataFlowIR.Edge>()
                var highestScope: DataFlowIR.Node.Scope? = null
                expressionValuesExtractor.forEachValue(expression) {
                    values += it
                    if (it != expression || values.size > 1) {
                        val edge = expressionToScopedEdge(it)
                        val scope = edge.scope
                        if (highestScope == null || highestScope!!.depth > scope.depth)
                            highestScope = scope
                        edges += edge.value
                    }
                }
                if (values.size == 1 && values[0] == expression) {
                    highestScope = expressionsScopes[expression] ?: error("Unknown expression: ${expression.dump()}")
                }
                if (values.size == 0)
                    highestScope = rootScope
                val node = if (values.size != 1) {
                    DataFlowIR.Node.Variable(
                            values = edges,
                            type   = symbolTable.mapType(expression.type),
                            kind   = DataFlowIR.VariableKind.Temporary
                    )
                } else {
                    val value = values[0]
                    if (value != expression) {
                        val edge = edges[0]
                        if (edge.castToType == null)
                            edge.node
                        else
                            DataFlowIR.Node.Variable(
                                    values = listOf(edge),
                                    type   = symbolTable.mapType(expression.type),
                                    kind   = DataFlowIR.VariableKind.Temporary
                            )
                    } else {
                        when (value) {
                            is IrGetValue -> getNode(value).value

                            is IrVararg -> DataFlowIR.Node.Const(symbolTable.mapType(value.type))

                            is IrFunctionReference -> {
                                val callee = value.symbol.owner
                                DataFlowIR.Node.FunctionReference(
                                        symbolTable.mapFunction(callee),
                                        symbolTable.mapType(value.type),
                                        /*TODO: substitute*/symbolTable.mapType(callee.returnType))
                            }

                            is IrConst<*> ->
                                if (value.value == null)
                                    DataFlowIR.Node.Null
                                else
                                    DataFlowIR.Node.SimpleConst(symbolTable.mapType(value.type), value.value!!)

                            is IrGetObjectValue -> {
                                val constructor = if (value.type.isNothing()) {
                                    // <Nothing> is not a singleton though its instance is get with <IrGetObject> operation.
                                    null
                                } else {
                                    val objectClass = value.symbol.owner
                                    if (objectClass is IrLazyClass) {
                                        // Singleton has a private constructor which is not deserialized.
                                        null
                                    } else {
                                        symbolTable.mapFunction(objectClass.constructors.single())
                                    }
                                }
                                DataFlowIR.Node.Singleton(symbolTable.mapType(value.type), constructor)
                            }

                            is IrConstructorCall -> {
                                val callee = value.symbol.owner
                                val arguments = value.getArguments().map { expressionToEdge(it.second) }
                                DataFlowIR.Node.NewObject(
                                        symbolTable.mapFunction(callee),
                                        arguments,
                                        symbolTable.mapClassReferenceType(callee.constructedClass),
                                        value
                                )
                            }

                            is IrCall -> when (value.symbol) {
                                getContinuationSymbol -> getContinuation().value

                                in arrayGetSymbols -> {
                                    val callee = value.symbol.owner
                                    val actualCallee = (value.superQualifierSymbol?.owner?.getOverridingOf(callee)
                                            ?: callee).target

                                    DataFlowIR.Node.ArrayRead(
                                            symbolTable.mapFunction(actualCallee),
                                            array = expressionToEdge(value.dispatchReceiver!!),
                                            index = expressionToEdge(value.getValueArgument(0)!!),
                                            type = mapReturnType(value.type, context.irBuiltIns.anyType),
                                            irCallSite = value)
                                }

                                in arraySetSymbols -> {
                                    val callee = value.symbol.owner
                                    val actualCallee = (value.superQualifierSymbol?.owner?.getOverridingOf(callee)
                                            ?: callee).target
                                    DataFlowIR.Node.ArrayWrite(
                                            symbolTable.mapFunction(actualCallee),
                                            array = expressionToEdge(value.dispatchReceiver!!),
                                            index = expressionToEdge(value.getValueArgument(0)!!),
                                            value = expressionToEdge(value.getValueArgument(1)!!),
                                            type = mapReturnType(value.getValueArgument(1)!!.type, context.irBuiltIns.anyType))
                                }

                                createUninitializedInstanceSymbol ->
                                    DataFlowIR.Node.AllocInstance(symbolTable.mapClassReferenceType(
                                            value.getTypeArgument(0)!!.getClass()!!
                                    ), value)

                                reinterpret -> getNode(value.extensionReceiver!!).value

                                initInstanceSymbol -> {
                                    val thiz = expressionToEdge(value.getValueArgument(0)!!)
                                    val initializer = value.getValueArgument(1) as IrConstructorCall
                                    val arguments = listOf(thiz) + initializer.getArguments().map { expressionToEdge(it.second) }
                                    val callee = initializer.symbol.owner
                                    DataFlowIR.Node.StaticCall(
                                            symbolTable.mapFunction(callee),
                                            arguments,
                                            symbolTable.mapClassReferenceType(callee.constructedClass),
                                            symbolTable.mapClassReferenceType(symbols.unit.owner),
                                            null
                                    )
                                }

                                else -> {
                                    val callee = value.symbol.owner
                                    val arguments = value.getArguments()
                                            .map { expressionToEdge(it.second) }
                                            .let {
                                                if (callee.isSuspend)
                                                    it + DataFlowIR.Edge(getContinuation().value, null)
                                                else
                                                    it
                                            }
                                    if (callee is IrConstructor) {
                                        error("Constructor call should be done with IrConstructorCall")
                                    } else {
                                        if (callee.isOverridable && value.superQualifierSymbol == null) {
                                            val owner = callee.parentAsClass
                                            val actualReceiverType = value.dispatchReceiver!!.type
                                            val actualReceiverClassifier = actualReceiverType.classifierOrFail

                                            val receiverType =
                                                    if (actualReceiverClassifier is IrTypeParameterSymbol
                                                            || !callee.isReal /* Could be a bridge. */)
                                                        symbolTable.mapClassReferenceType(owner)
                                                    else {
                                                        val actualClassAtCallsite =
                                                                (actualReceiverClassifier as IrClassSymbol).descriptor
//                                                        assert (DescriptorUtils.isSubclass(actualClassAtCallsite, owner.descriptor)) {
//                                                            "Expected an inheritor of ${owner.descriptor}, but was $actualClassAtCallsite"
//                                                        }
                                                        if (DescriptorUtils.isSubclass(actualClassAtCallsite, owner.descriptor)) {
                                                            symbolTable.mapClassReferenceType(actualReceiverClassifier.owner) // Box if inline class.
                                                        } else {
                                                            symbolTable.mapClassReferenceType(owner)
                                                        }
                                                    }
                                            if (owner.isInterface) {
                                                val calleeHash = callee.functionName.localHash.value
                                                DataFlowIR.Node.ItableCall(
                                                        symbolTable.mapFunction(callee.target),
                                                        receiverType,
                                                        calleeHash,
                                                        arguments,
                                                        mapReturnType(value.type, callee.target.returnType),
                                                        value
                                                )
                                            } else {
                                                val vtableIndex = context.getLayoutBuilder(owner).vtableIndex(callee)
                                                DataFlowIR.Node.VtableCall(
                                                        symbolTable.mapFunction(callee.target),
                                                        receiverType,
                                                        vtableIndex,
                                                        arguments,
                                                        mapReturnType(value.type, callee.target.returnType),
                                                        value
                                                )
                                            }
                                        } else {
                                            val actualCallee = (value.superQualifierSymbol?.owner?.getOverridingOf(callee) ?: callee).target
                                            DataFlowIR.Node.StaticCall(
                                                    symbolTable.mapFunction(actualCallee),
                                                    arguments,
                                                    actualCallee.dispatchReceiverParameter?.let { symbolTable.mapType(it.type) },
                                                    mapReturnType(value.type, actualCallee.returnType),
                                                    value
                                            )
                                        }
                                    }
                                }
                            }

                            is IrDelegatingConstructorCall -> {
                                val thisReceiver = (declaration as IrConstructor).constructedClass.thisReceiver!!
                                val thiz = IrGetValueImpl(SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, thisReceiver.type,
                                        thisReceiver.symbol)
                                val arguments = listOf(thiz) + value.getArguments().map { it.second }
                                DataFlowIR.Node.StaticCall(
                                        symbolTable.mapFunction(value.symbol.owner),
                                        arguments.map { expressionToEdge(it) },
                                        symbolTable.mapType(thiz.type),
                                        symbolTable.mapClassReferenceType(symbols.unit.owner),
                                        value
                                )
                            }

                            is IrGetField -> {
                                val receiver = value.receiver?.let { expressionToEdge(it) }
                                val receiverType = value.receiver?.let { symbolTable.mapType(it.type) }
                                val name = value.symbol.owner.name.asString()
                                DataFlowIR.Node.FieldRead(
                                        receiver,
                                        DataFlowIR.Field(
                                                receiverType,
                                                symbolTable.mapType(value.symbol.owner.type),
                                                name.localHash.value,
                                                takeName { name }
                                        ),
                                        mapReturnType(value.type, value.symbol.owner.type),
                                        value
                                )
                            }

                            is IrSetField -> {
                                val receiver = value.receiver?.let { expressionToEdge(it) }
                                val receiverType = value.receiver?.let { symbolTable.mapType(it.type) }
                                val name = value.symbol.owner.name.asString()
                                DataFlowIR.Node.FieldWrite(
                                        receiver,
                                        DataFlowIR.Field(
                                                receiverType,
                                                symbolTable.mapType(value.symbol.owner.type),
                                                name.localHash.value,
                                                takeName { name }
                                        ),
                                        expressionToEdge(value.value),
                                        mapReturnType(value.value.type, value.symbol.owner.type)
                                )
                            }

                            is IrTypeOperatorCall -> {
                                assert(!value.operator.isCast()) { "Casts should've been handled earlier" }
                                expressionToEdge(value.argument) // Put argument as a separate vertex.
                                DataFlowIR.Node.Const(symbolTable.mapType(value.type)) // All operators except casts are basically constants.
                            }

                            else -> TODO("Unknown expression: ${ir2stringWhole(value)}")
                        }
                    }
                }

                highestScope!!.nodes += node
                Scoped(node, highestScope!!)
            }
        }
    }
}