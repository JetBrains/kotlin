package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.isInterface
import org.jetbrains.kotlin.backend.konan.descriptors.target
import org.jetbrains.kotlin.backend.konan.ir.IrSuspendableExpression
import org.jetbrains.kotlin.backend.konan.ir.IrSuspensionPoint
import org.jetbrains.kotlin.backend.konan.llvm.functionName
import org.jetbrains.kotlin.backend.konan.llvm.localHash
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrTypeOperatorCallImpl
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

private fun computeErasure(type: KotlinType, erasure: MutableList<KotlinType>) {
    val descriptor = type.constructor.declarationDescriptor
    when (descriptor) {
        is ClassDescriptor -> erasure += type.makeNotNullable()
        is TypeParameterDescriptor -> {
            descriptor.upperBounds.forEach {
                computeErasure(it, erasure)
            }
        }
        else -> TODO(descriptor.toString())
    }
}

internal fun KotlinType.erasure(): List<KotlinType> {
    val result = mutableListOf<KotlinType>()
    computeErasure(this, result)
    return result
}

private fun MemberScope.getOverridingOf(function: FunctionDescriptor) = when (function) {
    is PropertyGetterDescriptor ->
        this.getContributedVariables(function.correspondingProperty.name, NoLookupLocation.FROM_BACKEND)
                .firstOrNull { OverridingUtil.overrides(it, function.correspondingProperty) }?.getter

    is PropertySetterDescriptor ->
        this.getContributedVariables(function.correspondingProperty.name, NoLookupLocation.FROM_BACKEND)
                .firstOrNull { OverridingUtil.overrides(it, function.correspondingProperty) }?.setter

    else -> this.getContributedFunctions(function.name, NoLookupLocation.FROM_BACKEND)
            .firstOrNull { OverridingUtil.overrides(it, function) }
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
                val descriptor = element.descriptor
                if (descriptor is VariableDescriptor && !seen.contains(descriptor))
                    dfs(descriptor, seen, result)
            }
        }
    }
}

private class ExpressionValuesExtractor(val returnableBlockValues: Map<IrReturnableBlock, List<IrExpression>>,
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
                    forEachValue(expression.statements.last() as IrExpression, block)
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
                if ((expression.type.isUnit() || expression.type.isNothing())) {
                    block(IrGetObjectValueImpl(expression.startOffset, expression.endOffset,
                            expression.type, (expression.type.constructor.declarationDescriptor as ClassDescriptor)))
                }
                else TODO(ir2stringWhole(expression))
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

    private val module = DataFlowIR.Module(irModule.descriptor)
    private val symbolTable = DataFlowIR.SymbolTable(context, irModule, module)

    // Possible values of a returnable block.
    private val returnableBlockValues = mutableMapOf<IrReturnableBlock, MutableList<IrExpression>>()

    // All suspension points within specified suspendable expression.
    private val suspendableExpressionValues = mutableMapOf<IrSuspendableExpression, MutableList<IrSuspensionPoint>>()

    private val expressionValuesExtractor = ExpressionValuesExtractor(returnableBlockValues, suspendableExpressionValues)

    fun build(): ModuleDFG {
        val functions = mutableMapOf<DataFlowIR.FunctionSymbol, DataFlowIR.Function>()
        irModule.accept(object : IrElementVisitorVoid {

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitFunction(declaration: IrFunction) {
                declaration.body?.let {
                    DEBUG_OUTPUT(1) {
                        println("Analysing function ${declaration.descriptor}")
                        println("IR: ${ir2stringWhole(declaration)}")
                    }
                    analyze(declaration.descriptor, it)
                }
            }

            override fun visitField(declaration: IrField) {
                declaration.initializer?.let {
                    DEBUG_OUTPUT(1) {
                        println("Analysing global field ${declaration.descriptor}")
                        println("IR: ${ir2stringWhole(declaration)}")
                    }
                    analyze(declaration.descriptor, it)
                }
            }

            private fun analyze(descriptor: CallableDescriptor, body: IrElement) {
                // Find all interesting expressions, variables and functions.
                val visitor = ElementFinderVisitor()
                body.acceptVoid(visitor)

                DEBUG_OUTPUT(1) {
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

                DEBUG_OUTPUT(1) {
                    println("SECOND PHASE")
                    visitor.variableValues.elementData.forEach { t, u ->
                        println("VAR $t:")
                        u.forEach {
                            println("    ${ir2stringWhole(it)}")
                        }
                    }
                }

                val function = FunctionDFGBuilder(expressionValuesExtractor, visitor.variableValues,
                        descriptor, visitor.expressions, visitor.returnValues).build()

                DEBUG_OUTPUT(1) {
                    function.debugOutput()
                }

                functions.put(function.symbol, function)
            }
        }, data = null)

        DEBUG_OUTPUT(2) {
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

            if (expression is IrReturnableBlock) {
                returnableBlocks.put(expression.descriptor, expression)
                returnableBlockValues.put(expression, mutableListOf())
            }
            if (expression is IrSuspendableExpression) {
                suspendableExpressionStack.push(expression)
                suspendableExpressionValues.put(expression, mutableListOf())
            }
            if (expression is IrSuspensionPoint)
                suspendableExpressionValues[suspendableExpressionStack.peek()!!]!!.add(expression)
            super.visitExpression(expression)
            if (expression is IrReturnableBlock)
                returnableBlocks.remove(expression.descriptor)
            if (expression is IrSuspendableExpression)
                suspendableExpressionStack.pop()
        }

        override fun visitSetField(expression: IrSetField) {
            expressions += expression
            super.visitSetField(expression)
        }

        // TODO: hack to overcome bad code in InlineConstructorsTransformation.
        private val FQ_NAME_INLINE_CONSTRUCTOR = FqName("konan.internal.InlineConstructor")

        override fun visitReturn(expression: IrReturn) {
            val returnableBlock = returnableBlocks[expression.returnTarget]
            if (returnableBlock != null) {
                returnableBlockValues[returnableBlock]!!.add(expression.value)
            } else { // Non-local return.
                if (!expression.type.isUnit()) {
                    if (!expression.returnTarget.annotations.hasAnnotation(FQ_NAME_INLINE_CONSTRUCTOR)) // Not inline constructor.
                        returnValues += expression.value
                }
            }
            super.visitReturn(expression)
        }

        override fun visitSetVariable(expression: IrSetVariable) {
            super.visitSetVariable(expression)
            assignVariable(expression.descriptor, expression.value)
        }

        override fun visitVariable(declaration: IrVariable) {
            variableValues.addEmpty(declaration.descriptor)
            super.visitVariable(declaration)
            declaration.initializer?.let { assignVariable(declaration.descriptor, it) }
        }
    }

    private val doResumeFunctionDescriptor = context.getInternalClass("CoroutineImpl").unsubstitutedMemberScope
            .getContributedFunctions(Name.identifier("doResume"), NoLookupLocation.FROM_BACKEND).single()
    private val getContinuationSymbol = context.ir.symbols.getContinuation

    private inner class FunctionDFGBuilder(val expressionValuesExtractor: ExpressionValuesExtractor,
                                           val variableValues: VariableValues,
                                           val descriptor: CallableDescriptor,
                                           val expressions: List<IrExpression>,
                                           val returnValues: List<IrExpression>) {

        private val allParameters = (descriptor as? FunctionDescriptor)?.allParameters ?: emptyList()
        private val templateParameters = allParameters.withIndex().associateBy({ it.value }, { DataFlowIR.Node.Parameter(it.index) })

        private val continuationParameter =
                if (descriptor.isSuspend)
                    DataFlowIR.Node.Parameter(allParameters.size)
                else {
                    if (doResumeFunctionDescriptor in descriptor.overriddenDescriptors) // <this> is a CoroutineImpl inheritor.
                        templateParameters[descriptor.dispatchReceiverParameter!!]      // It is its own continuation.
                    else null
                }

        private fun getContinuation() = continuationParameter ?: error("Function $descriptor has no continuation parameter")

        private val nodes = mutableMapOf<IrExpression, DataFlowIR.Node>()
        private val variables = variableValues.elementData.keys.associateBy(
                { it },
                { DataFlowIR.Node.Variable(mutableListOf(), false) }
        )

        fun build(): DataFlowIR.Function {
            expressions.forEach { getNode(it) }
            val returnsNode = DataFlowIR.Node.Variable(returnValues.map { expressionToEdge(it) }, true)
            variables.forEach { descriptor, node ->
                variableValues.elementData[descriptor]!!.forEach {
                    node.values += expressionToEdge(it)
                }
            }
            val allNodes = nodes.values + variables.values + templateParameters.values + returnsNode +
                    (if (descriptor.isSuspend) listOf(continuationParameter!!) else emptyList())

            return DataFlowIR.Function(
                    symbol              = symbolTable.mapFunction(descriptor),
                    isGlobalInitializer = descriptor is PropertyDescriptor,
                    numberOfParameters  = templateParameters.size + if (descriptor.isSuspend) 1 else 0,
                    body                = DataFlowIR.FunctionBody(allNodes.distinct().toList(), returnsNode)
            )
        }

        private fun expressionToEdge(expression: IrExpression) =
                if (expression is IrTypeOperatorCall && expression.operator.isCast())
                    DataFlowIR.Edge(getNode(expression.argument), symbolTable.mapType(expression.typeOperand))
                else DataFlowIR.Edge(getNode(expression), null)

        private fun getNode(expression: IrExpression): DataFlowIR.Node {
            if (expression is IrGetValue) {
                val descriptor = expression.descriptor
                if (descriptor is ParameterDescriptor)
                    return templateParameters[descriptor]!!
                return variables[descriptor as VariableDescriptor]!!
            }
            return nodes.getOrPut(expression) {
                DEBUG_OUTPUT(1) {
                    println("Converting expression")
                    println(ir2stringWhole(expression))
                }
                val values = mutableListOf<IrExpression>()
                expressionValuesExtractor.forEachValue(expression) { values += it }
                if (values.size != 1) {
                    DataFlowIR.Node.Variable(values.map { expressionToEdge(it) }, true)
                } else {
                    val value = values[0]
                    if (value != expression) {
                        val edge = expressionToEdge(value)
                        if (edge.castToType == null)
                            edge.node
                        else
                            DataFlowIR.Node.Variable(listOf(edge), true)
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
                                    else symbolTable.mapFunction(value.descriptor.constructors.single())
                            )

                            is IrCall -> {
                                if (value.symbol == getContinuationSymbol) {
                                    getContinuation()
                                } else {
                                    val callee = value.descriptor
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
                                                symbolTable.mapClass(callee.constructedClass)
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
                                                        symbolTable.mapType(callee.returnType!!),
                                                        value
                                                )
                                            } else {
                                                val vtableIndex = vTableBuilder.vtableIndex(callee)
                                                assert(vtableIndex >= 0, { "Unable to find function $callee in vtable of $owner" })
                                                DataFlowIR.Node.VtableCall(
                                                        symbolTable.mapFunction(callee.target),
                                                        symbolTable.mapClass(owner),
                                                        vtableIndex,
                                                        arguments,
                                                        symbolTable.mapType(callee.returnType!!),
                                                        value
                                                )
                                            }
                                        } else {
                                            val actualCallee = (value.superQualifier?.unsubstitutedMemberScope?.getOverridingOf(callee) ?: callee).target
                                            DataFlowIR.Node.StaticCall(
                                                    symbolTable.mapFunction(actualCallee),
                                                    arguments,
                                                    symbolTable.mapType(actualCallee.returnType!!),
                                                    actualCallee.dispatchReceiverParameter?.let { symbolTable.mapType(it.type) }
                                            )
                                        }
                                    }
                                }
                            }

                            is IrDelegatingConstructorCall -> {
                                val thiz = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                        (descriptor as ConstructorDescriptor).constructedClass.thisAsReceiverParameter)
                                val arguments = listOf(thiz) + value.getArguments().map { it.second }
                                DataFlowIR.Node.StaticCall(
                                        symbolTable.mapFunction(value.descriptor),
                                        arguments.map { expressionToEdge(it) },
                                        symbolTable.mapClass(context.builtIns.unit),
                                        symbolTable.mapType(thiz.type)
                                )
                            }

                            is IrGetField -> {
                                val receiver = value.receiver?.let { expressionToEdge(it) }
                                val receiverType = value.receiver?.let { symbolTable.mapType(it.type) }
                                DataFlowIR.Node.FieldRead(
                                        receiver,
                                        DataFlowIR.Field(
                                                receiverType,
                                                value.descriptor.name.asString()
                                        )
                                )
                            }

                            is IrSetField -> {
                                val receiver = value.receiver?.let { expressionToEdge(it) }
                                val receiverType = value.receiver?.let { symbolTable.mapType(it.type) }
                                DataFlowIR.Node.FieldWrite(
                                        receiver,
                                        DataFlowIR.Field(
                                                receiverType,
                                                value.descriptor.name.asString()
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