/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.atMostOne
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.allOverriddenDescriptors
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.descriptors.target
import org.jetbrains.kotlin.backend.konan.ir.IrSuspendableExpression
import org.jetbrains.kotlin.backend.konan.ir.IrSuspensionPoint
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.constructedClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.util.OperatorNameConventions

private fun computeErasure(type: KotlinType, context: Context, erasure: MutableList<ClassDescriptor>) {
    val irClass = context.ir.getClass(type)
    if (irClass != null) {
        erasure += irClass
    } else {
        val descriptor = type.constructor.declarationDescriptor
        if (descriptor is TypeParameterDescriptor) {
            descriptor.upperBounds.forEach {
                computeErasure(it, context, erasure)
            }
        } else {
            TODO(descriptor.toString())
        }
    }
}

internal fun KotlinType.erasure(context: Context): List<ClassDescriptor> {
    val result = mutableListOf<ClassDescriptor>()
    computeErasure(this, context, result)
    return result
}

private fun IrClass.getOverridingOf(function: FunctionDescriptor) = (function as? SimpleFunctionDescriptor)?.let {
    it.allOverriddenDescriptors.atMostOne { it.parent == this }
}

private fun IrTypeOperator.isCast() =
        this == IrTypeOperator.CAST || this == IrTypeOperator.IMPLICIT_CAST || this == IrTypeOperator.SAFE_CAST


private class VariableValues {
    val elementData = HashMap<VariableDescriptor, MutableSet<IrExpression>>()

    fun addEmpty(variable: VariableDescriptor) =
            elementData.getOrPut(variable, { mutableSetOf() })

    fun add(variable: VariableDescriptor, element: IrExpression) =
            elementData[variable]?.add(element)

    fun add(variable: VariableDescriptor, elements: Set<IrExpression>) =
            elementData[variable]?.addAll(elements)

    fun get(variable: VariableDescriptor): Set<IrExpression>? =
            elementData[variable]

    fun computeClosure() {
        elementData.forEach { key, _ ->
            add(key, computeValueClosure(key))
        }
    }

    // Computes closure of all possible values for given variable.
    private fun computeValueClosure(value: VariableDescriptor): Set<IrExpression> {
        val result = mutableSetOf<IrExpression>()
        val seen = mutableSetOf<VariableDescriptor>()
        dfs(value, seen, result)
        return result
    }

    private fun dfs(value: VariableDescriptor, seen: MutableSet<VariableDescriptor>, result: MutableSet<IrExpression>) {
        seen += value
        val elements = elementData[value]
                ?: return
        for (element in elements) {
            if (element !is IrGetValue)
                result += element
            else {
                val descriptor = element.symbol.owner
                if (descriptor is VariableDescriptor && !seen.contains(descriptor))
                    dfs(descriptor, seen, result)
            }
        }
    }
}

private class ExpressionValuesExtractor(val context: Context,
                                        val returnableBlockValues: Map<IrReturnableBlock, List<IrExpression>>,
                                        val suspendableExpressionValues: Map<IrSuspendableExpression, List<IrSuspensionPoint>>) {

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
                            expression = (expression.statements.last() as? IrExpression)
                                    ?: IrGetObjectValueImpl(expression.startOffset, expression.endOffset,
                                            context.builtIns.unitType, context.ir.symbols.unit),
                            block      = block
                    )
            }

            is IrWhen -> expression.branches.forEach { forEachValue(it.result, block) }

            is IrMemberAccessExpression -> block(expression)

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

            else -> {
                val classSymbol = when {
                    expression.type.isUnit() -> context.ir.symbols.unit
                    expression.type.isNothing() -> context.ir.symbols.nothing
                    else -> TODO(ir2stringWhole(expression))
                }

                block(IrGetObjectValueImpl(expression.startOffset, expression.endOffset,
                        expression.type, classSymbol))
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
                assert (body != null || declaration.symbol.constructedClass.kind == ClassKind.ANNOTATION_CLASS) {
                    "Non-annotation class constructor has empty body"
                }
                DEBUG_OUTPUT(0) {
                    println("Analysing function ${declaration.descriptor}")
                    println("IR: ${ir2stringWhole(declaration)}")
                }
                analyze(declaration, body)
            }

            override fun visitFunction(declaration: IrFunction) {
                declaration.body?.let {
                    DEBUG_OUTPUT(0) {
                        println("Analysing function ${declaration.descriptor}")
                        println("IR: ${ir2stringWhole(declaration)}")
                    }
                    analyze(declaration, it)
                }
            }

            override fun visitField(declaration: IrField) {
                declaration.initializer?.let {
                    DEBUG_OUTPUT(0) {
                        println("Analysing global field ${declaration.descriptor}")
                        println("IR: ${ir2stringWhole(declaration)}")
                    }
                    analyze(declaration, IrSetFieldImpl(it.startOffset, it.endOffset, declaration.symbol, null, it.expression))
                }
            }

            private fun analyze(descriptor: DeclarationDescriptor, body: IrElement?) {
                // Find all interesting expressions, variables and functions.
                val visitor = ElementFinderVisitor()
                body?.acceptVoid(visitor)

                DEBUG_OUTPUT(0) {
                    println("FIRST PHASE")
                    visitor.variableValues.elementData.forEach { t, u ->
                        println("VAR $t:")
                        u.forEach {
                            println("    ${ir2stringWhole(it)}")
                        }
                    }
                    visitor.expressions.forEach { t ->
                        println("EXP ${ir2stringWhole(t)}")
                    }
                }

                // Compute transitive closure of possible values for variables.
                visitor.variableValues.computeClosure()

                DEBUG_OUTPUT(0) {
                    println("SECOND PHASE")
                    visitor.variableValues.elementData.forEach { t, u ->
                        println("VAR $t:")
                        u.forEach {
                            println("    ${ir2stringWhole(it)}")
                        }
                    }
                }

                val function = FunctionDFGBuilder(expressionValuesExtractor, visitor.variableValues,
                        descriptor, visitor.expressions, visitor.returnValues, visitor.thrownValues, visitor.catchParameters).build()

                DEBUG_OUTPUT(0) {
                    function.debugOutput()
                }

                functions.put(function.symbol, function)
            }
        }, data = null)

        DEBUG_OUTPUT(1) {
            println("SYMBOL TABLE:")
            symbolTable.classMap.forEach { descriptor, type ->
                println("    DESCRIPTOR: $descriptor")
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

        val expressions = mutableListOf<IrExpression>()
        val variableValues = VariableValues()
        val returnValues = mutableListOf<IrExpression>()
        val thrownValues = mutableListOf<IrExpression>()
        val catchParameters = mutableSetOf<VariableDescriptor>()

        private val returnableBlocks = mutableMapOf<FunctionDescriptor, IrReturnableBlock>()
        private val suspendableExpressionStack = mutableListOf<IrSuspendableExpression>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        private fun assignVariable(variable: VariableDescriptor, value: IrExpression) {
            expressionValuesExtractor.forEachValue(value) {
                variableValues.add(variable, it)
            }
        }

        override fun visitExpression(expression: IrExpression) {
            when (expression) {
                is IrMemberAccessExpression,
                is IrGetField,
                is IrGetObjectValue,
                is IrVararg,
                is IrConst<*>,
                is IrTypeOperatorCall ->
                    expressions += expression
            }

            if (expression is IrCall && expression.symbol == scheduleImplSymbol) {
                // Producer of scheduleImpl is called externally, we need to reflect this somehow.
                val producerInvocation = IrCallImpl(expression.startOffset, expression.endOffset,
                        scheduleImplProducerInvoke.symbol, scheduleImplProducerInvoke.descriptor)
                producerInvocation.dispatchReceiver = expression.getValueArgument(2)
                expressions += producerInvocation
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
            super.visitExpression(expression)
            if (expression is IrSuspendableExpression)
                suspendableExpressionStack.pop()
        }

        override fun visitSetField(expression: IrSetField) {
            expressions += expression
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
            variableValues.addEmpty(declaration)
            super.visitVariable(declaration)
            declaration.initializer?.let { assignVariable(declaration, it) }
        }
    }

    private val doResumeFunctionSymbol =
            context.ir.symbols.coroutineImpl.owner.declarations
                    .filterIsInstance<IrSimpleFunction>().single { it.name.asString() == "doResume" }.symbol

    private val getContinuationSymbol = context.ir.symbols.getContinuation

    private val arrayGetSymbol = context.ir.symbols.arrayGet
    private val arraySetSymbol = context.ir.symbols.arraySet
    private val scheduleImplSymbol = context.ir.symbols.scheduleImpl
    private val scheduleImplProducerClassSymbol = context.ir.symbols.functions[0]
    private val scheduleImplProducerParam = scheduleImplSymbol.descriptor.valueParameters[2].also {
        assert(it.name.asString() == "producer")
        assert(it.type.constructor.declarationDescriptor == scheduleImplProducerClassSymbol.descriptor)
    }
    private val scheduleImplProducerInvoke = scheduleImplProducerClassSymbol.owner.simpleFunctions()
            .single { it.name == OperatorNameConventions.INVOKE }

    private inner class FunctionDFGBuilder(val expressionValuesExtractor: ExpressionValuesExtractor,
                                           val variableValues: VariableValues,
                                           val descriptor: DeclarationDescriptor,
                                           val expressions: List<IrExpression>,
                                           val returnValues: List<IrExpression>,
                                           val thrownValues: List<IrExpression>,
                                           val catchParameters: Set<VariableDescriptor>) {

        private val allParameters = (descriptor as? FunctionDescriptor)?.allParameters ?: emptyList()
        private val templateParameters = allParameters.withIndex().associateBy({ it.value }, { DataFlowIR.Node.Parameter(it.index) })

        private val continuationParameter = if (descriptor !is IrSimpleFunction) {
            null
        } else if (descriptor.isSuspend) {
            DataFlowIR.Node.Parameter(allParameters.size)
        } else if (doResumeFunctionSymbol in descriptor.overriddenSymbols) { // <this> is a CoroutineImpl inheritor.
            templateParameters[descriptor.dispatchReceiverParameter!!]       // It is its own continuation.
        } else {
            null
        }

        private fun getContinuation() = continuationParameter ?: error("Function $descriptor has no continuation parameter")

        private val nodes = mutableMapOf<IrExpression, DataFlowIR.Node>()
        private val variables = variableValues.elementData.keys.associate {
                it to DataFlowIR.Node.Variable(
                        values = mutableListOf(),
                        type   = symbolTable.mapType(it.type),
                        kind   = if (catchParameters.contains(it))
                                     DataFlowIR.VariableKind.CatchParameter
                                 else DataFlowIR.VariableKind.Ordinary
                )
        }

        fun build(): DataFlowIR.Function {
            val isSuspend = descriptor is IrSimpleFunction && descriptor.isSuspend

            expressions.forEach { getNode(it) }

            val returnNodeType = when (descriptor) {
                is IrField -> descriptor.type
                is IrFunction -> descriptor.returnType
                else -> error(descriptor)
            }

            val returnsNode = DataFlowIR.Node.Variable(
                    values = returnValues.map { expressionToEdge(it) },
                    type   = symbolTable.mapType(returnNodeType),
                    kind   = DataFlowIR.VariableKind.Temporary
            )
            val throwsNode = DataFlowIR.Node.Variable(
                    values = thrownValues.map { expressionToEdge(it) },
                    type   = symbolTable.mapClass(context.ir.symbols.throwable.owner),
                    kind   = DataFlowIR.VariableKind.Temporary
            )
            variables.forEach { descriptor, node ->
                variableValues.elementData[descriptor]!!.forEach {
                    node.values += expressionToEdge(it)
                }
            }
            val allNodes = nodes.values + variables.values + templateParameters.values + returnsNode + throwsNode +
                    (if (isSuspend) listOf(continuationParameter!!) else emptyList())

            return DataFlowIR.Function(
                    symbol         = symbolTable.mapFunction(descriptor),
                    body           = DataFlowIR.FunctionBody(allNodes.distinct().toList(), returnsNode, throwsNode)
            )
        }

        private fun expressionToEdge(expression: IrExpression) =
                if (expression is IrTypeOperatorCall && expression.operator.isCast())
                    DataFlowIR.Edge(getNode(expression.argument), symbolTable.mapType(expression.typeOperand))
                else DataFlowIR.Edge(getNode(expression), null)

        private fun getNode(expression: IrExpression): DataFlowIR.Node {
            if (expression is IrGetValue) {
                val descriptor = expression.symbol.owner
                if (descriptor is IrValueParameter)
                    return templateParameters[descriptor]!!
                return variables[descriptor]!!
            }
            return nodes.getOrPut(expression) {
                DEBUG_OUTPUT(0) {
                    println("Converting expression")
                    println(ir2stringWhole(expression))
                }
                val values = mutableListOf<IrExpression>()
                expressionValuesExtractor.forEachValue(expression) { values += it }
                if (values.size != 1) {
                    DataFlowIR.Node.Variable(
                            values = values.map { expressionToEdge(it) },
                            type   = symbolTable.mapType(expression.type),
                            kind   = DataFlowIR.VariableKind.Temporary
                    )
                } else {
                    val value = values[0]
                    if (value != expression) {
                        val edge = expressionToEdge(value)
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
                            is IrGetValue -> getNode(value)

                            is IrVararg,
                            is IrConst<*>,
                            is IrFunctionReference -> DataFlowIR.Node.Const(symbolTable.mapType(value.type))

                            is IrGetObjectValue -> DataFlowIR.Node.Singleton(
                                    symbolTable.mapType(value.type),
                                    if (value.type.isNothing()) // <Nothing> is not a singleton though its instance is get with <IrGetObject> operation.
                                        null
                                    else symbolTable.mapFunction(value.symbol.owner.constructors.single())
                            )

                            is IrCall -> when (value.symbol) {
                                getContinuationSymbol -> getContinuation()

                                arrayGetSymbol -> DataFlowIR.Node.ArrayRead(expressionToEdge(value.dispatchReceiver!!),
                                        expressionToEdge(value.getValueArgument(0)!!), value)

                                arraySetSymbol -> DataFlowIR.Node.ArrayWrite(expressionToEdge(value.dispatchReceiver!!),
                                        expressionToEdge(value.getValueArgument(0)!!), expressionToEdge(value.getValueArgument(1)!!))

                                else -> {
                                    val callee = value.symbol.owner as IrFunction
                                    val arguments = value.getArguments()
                                            .map { expressionToEdge(it.second) }
                                            .let {
                                                if (callee.isSuspend)
                                                    it + DataFlowIR.Edge(getContinuation(), null)
                                                else
                                                    it
                                            }
                                    if (callee is ConstructorDescriptor) {
                                        DataFlowIR.Node.NewObject(
                                                symbolTable.mapFunction(callee),
                                                arguments,
                                                symbolTable.mapClass(callee.constructedClass),
                                                value
                                        )
                                    } else {
                                        if (callee.isOverridable && value.superQualifier == null) {
                                            val owner = callee.containingDeclaration as ClassDescriptor
                                            val vTableBuilder = context.getVtableBuilder(owner)
                                            if (owner.isInterface) {
                                                DataFlowIR.Node.ItableCall(
                                                        symbolTable.mapFunction(callee.target),
                                                        symbolTable.mapClass(owner),
                                                        callee.functionName.localHash.value,
                                                        arguments,
                                                        value
                                                )
                                            } else {
                                                val vtableIndex = vTableBuilder.vtableIndex(callee as IrSimpleFunction)
                                                assert(vtableIndex >= 0, { "Unable to find function $callee in vtable of $owner" })
                                                DataFlowIR.Node.VtableCall(
                                                        symbolTable.mapFunction(callee.target),
                                                        symbolTable.mapClass(owner),
                                                        vtableIndex,
                                                        arguments,
                                                        value
                                                )
                                            }
                                        } else {
                                            val actualCallee = (value.superQualifierSymbol?.owner?.getOverridingOf(callee) ?: callee).target
                                            DataFlowIR.Node.StaticCall(
                                                    symbolTable.mapFunction(actualCallee),
                                                    arguments,
                                                    actualCallee.dispatchReceiverParameter?.let { symbolTable.mapType(it.type) },
                                                    value
                                            )
                                        }
                                    }
                                }
                            }

                            is IrDelegatingConstructorCall -> {
                                val thiz = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                        (descriptor as ConstructorDescriptor).constructedClass.thisReceiver!!.symbol)
                                val arguments = listOf(thiz) + value.getArguments().map { it.second }
                                DataFlowIR.Node.StaticCall(
                                        symbolTable.mapFunction(value.symbol.owner),
                                        arguments.map { expressionToEdge(it) },
                                        symbolTable.mapType(thiz.type),
                                        value
                                )
                            }

                            is IrGetField -> {
                                val receiver = value.receiver?.let { expressionToEdge(it) }
                                val receiverType = value.receiver?.let { symbolTable.mapType(it.type) }
                                val name = value.descriptor.name.asString()
                                DataFlowIR.Node.FieldRead(
                                        receiver,
                                        DataFlowIR.Field(
                                                receiverType,
                                                name.localHash.value,
                                                takeName { name }
                                        )
                                )
                            }

                            is IrSetField -> {
                                val receiver = value.receiver?.let { expressionToEdge(it) }
                                val receiverType = value.receiver?.let { symbolTable.mapType(it.type) }
                                val name = value.descriptor.name.asString()
                                DataFlowIR.Node.FieldWrite(
                                        receiver,
                                        DataFlowIR.Field(
                                                receiverType,
                                                name.localHash.value,
                                                takeName { name }
                                        ),
                                        expressionToEdge(value.value)
                                )
                            }

                            is IrTypeOperatorCall -> {
                                assert(!value.operator.isCast(), { "Casts should've been handled earlier" })
                                expressionToEdge(value.argument) // Put argument as a separate vertex.
                                DataFlowIR.Node.Const(symbolTable.mapType(value.type)) // All operators except casts are basically constants.
                            }

                            else -> TODO("Unknown expression: ${ir2stringWhole(value)}")
                        }
                    }
                }
            }
        }
    }
}