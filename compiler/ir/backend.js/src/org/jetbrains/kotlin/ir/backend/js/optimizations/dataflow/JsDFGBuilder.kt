/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid

private fun IrTypeOperator.isCast() =
    this == IrTypeOperator.CAST || this == IrTypeOperator.IMPLICIT_CAST || this == IrTypeOperator.SAFE_CAST

/**
 * Enumerates the possible value-producing expressions of a compound IR expression.
 *
 * [ElementFinderVisitor] records the non-structural results of returnable blocks and suspendable
 * expressions before graph construction. This extractor then recursively removes those
 * containers, transparent casts, and statement-only expressions from value flow.
 */
private class ExpressionValuesExtractor(context: JsIrBackendContext) {
    val returnableBlockValues = mutableMapOf<IrReturnableBlock, MutableList<IrExpression>>()
    val suspendableExpressionValues = mutableMapOf<IrSuspendableExpression, MutableList<IrSuspensionPoint>>()

    val unit: IrExpression = IrGetObjectValueImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        type = context.irBuiltIns.unitType,
        symbol = context.irBuiltIns.unitClass,
    )

    fun forEachValue(expression: IrExpression, block: (IrExpression) -> Unit) {
        when (expression) {
            is IrReturnableBlock -> returnableBlockValues[expression]!!.forEach { forEachValue(it, block) }

            is IrSuspendableExpression -> {
                suspendableExpressionValues[expression]!!.forEach { forEachValue(it, block) }
                forEachValue(expression.result, block)
            }

            is IrSuspensionPoint -> {
                forEachValue(expression.result, block)
                forEachValue(expression.resumeResult, block)
            }

            is IrContainerExpression -> if (expression.statements.isNotEmpty()) {
                val expression = (expression.statements.last() as? IrExpression) ?: unit
                forEachValue(expression, block)
            }

            is IrWhen -> expression.branches.forEach { forEachValue(it.result, block) }

            is IrTypeOperatorCall -> {
                if (expression.operator.isCast()) {
                    // A pure cast changes only the static type.
                    forEachValue(expression.argument, block)
                } else {
                    block(expression)
                }
            }

            is IrTry -> {
                forEachValue(expression.tryResult, block)
                expression.catches.forEach { forEachValue(it.result, block) }
            }

            is IrVararg,
            is IrClassReference,
            is IrMemberAccessExpression<*>, is IrGetValue, is IrGetObjectValue,
            is IrGetField, is IrSetField, is IrConst, is IrConstantValue, is IrRawFunctionReference,
            is IrDynamicMemberExpression, is IrDynamicOperatorExpression, is IrFunctionExpression,
                -> block(expression)

            else -> {
                if (expression.type.isUnit() || expression.type.isNothing()) {
                    // The side effect is visited separately; Unit and Nothing add no value flow.
                    return
                }
                // Preserve an unrecognized value so graph construction can escape its operands.
                block(expression)
            }
        }
    }
}

/** Resolves a direct call, including a super-qualified call, to the declaration that executes. */
private val IrCall.actualCallee: IrSimpleFunction
    get() {
        val callee = symbol.owner
        val overridden = superQualifierSymbol?.owner?.let { superClass ->
            callee.allOverridden().singleOrNull { it.parent == superClass }
        }
        return (overridden ?: callee).resolveDfgTarget()
    }

/**
 * Builds one [JsDataFlowIR.ExecutionUnit] from an IR function or static field initializer.
 *
 * Construction has two stages. [ElementFinderVisitor] first records possible expression values,
 * reaching definitions, loop nesting, exits, and escapes. Graph construction then creates nodes,
 * places each node in the shallowest scope containing its inputs, and materializes the collected
 * return, throw, field-write, and escape flows.
 *
 * The builder retains all intermediate state and is therefore single-use: create a new instance
 * for every function or field initializer.
 */
internal class JsBodyDFGBuilder(
    private val context: JsIrBackendContext,
    private val symbolTable: JsDataFlowIR.SymbolTable,
) {
    private val expressionValuesExtractor = ExpressionValuesExtractor(context)
    private val visitor = ElementFinderVisitor(expressionValuesExtractor)

    private val rootScope = JsDataFlowIR.Node.Scope(depth = 0)

    private val templateParameters = mutableMapOf<IrValueParameter, Scoped<JsDataFlowIR.Node.Parameter>>()

    private val nodes = mutableMapOf<IrExpression, Scoped<JsDataFlowIR.Node>>()
    private val variables = mutableMapOf<IrValueDeclaration, Scoped<JsDataFlowIR.Node.Variable>>()
    private val expressionsScopes = mutableMapOf<IrExpression, JsDataFlowIR.Node.Scope>()
    private val scopes = mutableMapOf<IrLoop, JsDataFlowIR.Node.Scope>()

    /** Builds the execution unit for [irFunction], including its return-value merge. */
    fun buildFunction(irFunction: IrFunction): JsDataFlowIR.Function {
        val body = requireNotNull(irFunction.body) {
            "No body for ${irFunction.render()}"
        }

        body.acceptVoid(visitor)
        visitor.variableValues.computeClosure()
        prepareBody(irFunction.parameters)

        val returns = JsDataFlowIR.Node.Variable(
            values = visitor.returnValues.map { it.toValueNode() },
            type = symbolTable.mapType(irFunction.returnType),
        )
        rootScope.nodes += returns

        return JsDataFlowIR.Function(
            symbol = symbolTable.mapFunction(irFunction),
            returns = returns,
            body = finishBody(),
        )
    }

    /**
     * Builds the execution unit for a static [irField] initializer and records its result as a
     * write to the field, rather than as a function return.
     */
    fun buildInitializer(irField: IrField): JsDataFlowIR.FieldInitializer {
        val expression = requireNotNull(irField.initializer) {
            "No initializer for ${irField.render()}"
        }.expression

        expression.acceptVoid(visitor)
        visitor.variableValues.computeClosure()
        prepareBody(parameters = emptyList())

        val field = symbolTable.mapField(irField)
        val value = expression.toValueNode()
        rootScope.nodes += JsDataFlowIR.Node.FieldWrite(
            receiver = null,
            field = field,
            value = value,
        )

        return JsDataFlowIR.FieldInitializer(
            field = field,
            value = value,
            body = finishBody(),
        )
    }

    private class Scoped<out T : Any>(val value: T, val scope: JsDataFlowIR.Node.Scope)

    private fun transformLoop(loop: IrLoop, parentLoop: IrLoop?): JsDataFlowIR.Node.Scope {
        scopes[loop]?.let { return it }
        val parentScope = parentLoop?.let { transformLoop(it, visitor.parentLoops[it]) } ?: rootScope
        val scope = JsDataFlowIR.Node.Scope(depth = parentScope.depth + 1)
        parentScope.nodes += scope
        scopes[loop] = scope
        return scope
    }

    /** Creates parameter, scope, expression, and local-variable nodes discovered by the prepass. */
    private fun prepareBody(parameters: List<IrValueParameter>) {
        parameters.forEachIndexed { index, parameter ->
            templateParameters[parameter] = Scoped(JsDataFlowIR.Node.Parameter(index), rootScope)
        }

        visitor.parentLoops.forEach { (key, value) -> transformLoop(key, value) }

        visitor.expressions.forEach { (key, value) ->
            val scope = if (value == null) rootScope else checkNotNull(scopes[value])
            expressionsScopes[key] = scope
        }
        expressionsScopes[expressionValuesExtractor.unit] = rootScope

        visitor.variableValues.elementData.forEach { (key, value) ->
            val scope = value.loop?.let { scopes[it] } ?: rootScope
            val node = JsDataFlowIR.Node.Variable(
                values = mutableListOf(),
                type = symbolTable.mapType(key.type),
            )
            scope.nodes += node
            variables[key] = Scoped(node, scope)
        }

        visitor.expressions.forEach { getNode(it.key) }
    }

    /**
     * Completes deferred value merges and adds the execution unit's throws and conservative
     * escapes before exposing the finished body.
     */
    private fun finishBody(): JsDataFlowIR.Body {
        val throwableIrClass = context.irBuiltIns.throwableClass.owner
        val throws = JsDataFlowIR.Node.Variable(
            values = visitor.thrownValues.map { it.toValueNode() },
            type = symbolTable.mapClassReferenceType(throwableIrClass),
        )

        val elements = visitor.variableValues.elementData
        variables.forEach { (key, value) ->
            val values = checkNotNull(elements[key]).values
            values.forEach { value.value.values += it.toValueNode() }
        }

        val escapes = buildList {
            for (escaped in visitor.escapedValues) {
                val node = variables[escaped]?.value ?: templateParameters[escaped]?.value ?: continue
                add(node)
            }
            visitor.escapedExpressions.forEach { add(it.toValueNode()) }
        }
        if (escapes.isNotEmpty()) {
            rootScope.nodes += JsDataFlowIR.Node.OpaqueValue(escapes)
        }

        rootScope.nodes += templateParameters.values.map { it.value }
        rootScope.nodes += throws

        val allScopes = listOf(rootScope) + scopes.values
        return JsDataFlowIR.Body(rootScope, allScopes, throws)
    }

    private fun IrExpression.toValueNode(): JsDataFlowIR.Node = getNode(this).value

    /**
     * Maps a call or read result to the type of the value visible at that use site.
     *
     * Usually this is [actualType]. When inline-class boxing or unboxing separates the declared
     * [returnType] from that representation, the mapped class must describe the representation
     * that can actually flow through the graph.
     */
    private fun mapReturnType(actualType: IrType, returnType: IrType): JsDataFlowIR.Type {
        val wrapperInlinedClass = context.inlineClassesUtils.getInlinedClass(returnType)
        val actualInlinedClass = context.inlineClassesUtils.getInlinedClass(actualType)

        return if (wrapperInlinedClass == null) {
            if (actualInlinedClass == null) symbolTable.mapType(actualType)
            else symbolTable.mapClassReferenceType(actualInlinedClass)
        } else {
            symbolTable.mapType(returnType)
        }
    }

    private fun getNode(expression: IrExpression): Scoped<JsDataFlowIR.Node> {
        if (expression is IrGetValue) {
            val valueDeclaration = expression.symbol.owner
            if (valueDeclaration is IrValueParameter) {
                return templateParameters[valueDeclaration]
                    ?: Scoped(JsDataFlowIR.Node.Const(symbolTable.mapType(expression.type)), rootScope)
            }
            return variables[valueDeclaration]
                ?: Scoped(JsDataFlowIR.Node.Const(symbolTable.mapType(expression.type)), rootScope)
        }
        return nodes.getOrPut(expression) {
            val values = mutableListOf<IrExpression>()
            val valueNodes = mutableListOf<JsDataFlowIR.Node>()
            var highestScope: JsDataFlowIR.Node.Scope? = null

            expressionValuesExtractor.forEachValue(expression) {
                values += it
                if (it != expression || values.size > 1) {
                    val scopedNode = getNode(it)
                    if (highestScope == null || highestScope!!.depth > scopedNode.scope.depth) {
                        highestScope = scopedNode.scope
                    }
                    valueNodes += scopedNode.value
                }
            }
            if (values.size == 1 && values[0] == expression) {
                highestScope = expressionsScopes[expression] ?: rootScope
            }
            if (values.isEmpty()) {
                highestScope = rootScope
            }
            val node = when {
                values.size != 1 -> JsDataFlowIR.Node.Variable(
                    values = valueNodes,
                    type = symbolTable.mapType(expression.type),
                )
                values[0] == expression -> toNode(expression = values[0])
                else -> valueNodes[0]
            }

            val scope = highestScope ?: error("No scope for expression: ${expression.render()}")
            scope.nodes += node
            Scoped(node, scope)
        }
    }

    private fun toCallNode(call: IrCall): JsDataFlowIR.Node {
        val callee = call.symbol.owner.let { it.findOverriddenMethodOfAny() ?: it }
        val arguments = call.alignedArgumentValues()

        if (!call.isVirtualCall) {
            val actualCallee = call.actualCallee
            val calleeSymbol = symbolTable.mapFunction(actualCallee)
            val returnType = mapReturnType(call.type, actualCallee.returnType)
            return if (call.symbol == context.symbols.constructCallableReferenceSymbol) {
                JsDataFlowIR.Node.CallableReferenceWrapper(calleeSymbol, arguments, returnType, irCall = call)
            } else {
                JsDataFlowIR.Node.StaticCall(calleeSymbol, arguments, returnType)
            }
        }

        val calleeTarget = callee.resolveDfgTarget()
        return JsDataFlowIR.Node.VirtualCall(
            callee = symbolTable.mapFunction(calleeTarget),
            arguments = arguments,
            returnType = mapReturnType(actualType = call.type, returnType = calleeTarget.returnType),
        )
    }

    private fun toNode(expression: IrExpression): JsDataFlowIR.Node = when (expression) {
        is IrGetValue -> getNode(expression).value

        is IrClassReference -> JsDataFlowIR.Node.Const(symbolTable.mapType(expression.type))

        is IrVararg -> JsDataFlowIR.Node.OpaqueValue(
            expression.elements.map { element ->
                val value = when (element) {
                    is IrSpreadElement -> element.expression
                    is IrExpression -> element
                    else -> error("Unexpected vararg element: ${element.render()}")
                }
                value.toValueNode()
            }
        )

        is IrRawFunctionReference -> JsDataFlowIR.Node.FunctionReference(
            symbol = symbolTable.mapFunction(expression.symbol.owner),
            type = symbolTable.mapType(expression.type),
        )

        is IrConst ->
            if (expression.value == null) JsDataFlowIR.Node.Null
            else JsDataFlowIR.Node.Const(symbolTable.mapType(expression.type))

        is IrConstantPrimitive ->
            if (expression.value.value == null) JsDataFlowIR.Node.Null
            else JsDataFlowIR.Node.Const(mapReturnType(actualType = expression.value.type, returnType = expression.type))

        is IrGetObjectValue -> JsDataFlowIR.Node.Singleton(type = symbolTable.mapType(expression.type))

        is IrConstructorCall -> {
            val callee = expression.symbol.owner
            JsDataFlowIR.Node.NewInstance(
                type = symbolTable.mapType(expression.type),
                constructor = symbolTable.mapFunction(callee),
                constructorArguments = expression.alignedArgumentValues(),
            )
        }

        is IrDelegatingConstructorCall -> {
            val callee = expression.symbol.owner
            JsDataFlowIR.Node.StaticCall(
                callee = symbolTable.mapFunction(callee),
                arguments = expression.alignedArgumentValues(),
                returnType = mapReturnType(actualType = expression.type, returnType = callee.returnType),
            )
        }

        is IrCall -> toCallNode(call = expression)

        is IrGetField -> JsDataFlowIR.Node.FieldRead(
            receiver = expression.receiver?.toValueNode(),
            field = symbolTable.mapField(expression.symbol.owner),
            type = mapReturnType(actualType = expression.type, returnType = expression.symbol.owner.type),
        )

        is IrSetField -> JsDataFlowIR.Node.FieldWrite(
            receiver = expression.receiver?.toValueNode(),
            field = symbolTable.mapField(expression.symbol.owner),
            value = expression.value.toValueNode(),
        )

        is IrTypeOperatorCall -> {
            assert(!expression.operator.isCast()) {
                "Casts should've been handled earlier"
            }
            JsDataFlowIR.Node.TypeCheck(
                argument = expression.argument.toValueNode(),
                operator = expression.operator
            )
        }

        is IrDynamicMemberExpression ->
            JsDataFlowIR.Node.DynamicAccess(receiver = expression.receiver.toValueNode(), arguments = emptyList())

        is IrDynamicOperatorExpression ->
            JsDataFlowIR.Node.DynamicAccess(
                receiver = expression.receiver.toValueNode(),
                arguments = expression.arguments.map { it.toValueNode() },
                operator = expression.operator,
            )

        is IrFunctionExpression ->
            // The prepass routes captured inputs to an OpaqueValue escape.
            JsDataFlowIR.Node.Const(symbolTable.mapType(expression.type))

        is IrConstantArray ->
            JsDataFlowIR.Node.OpaqueValue(expression.elements.map { it.toValueNode() })

        is IrConstantObject ->
            JsDataFlowIR.Node.OpaqueValue(expression.valueArguments.map { it.toValueNode() })

        else -> {
            // Preserve soundness for unrecognized IR by escaping every expression operand.
            val operands = mutableListOf<IrExpression>()
            expression.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    if (element is IrExpression) operands += element else element.acceptChildrenVoid(this)
                }
            })
            if (operands.isEmpty()) {
                JsDataFlowIR.Node.Const(type = symbolTable.mapType(expression.type))
            } else {
                JsDataFlowIR.Node.OpaqueValue(operands.map { it.toValueNode() })
            }
        }
    }

    /**
     * Returns arguments index-aligned with [JsDataFlowIR.FunctionSymbol.parameters].
     *
     * An absent argument, including a constructor receiver hole or an omitted default, becomes
     * an inert dynamic [JsDataFlowIR.Node.Const]. The placeholder prevents later arguments from
     * shifting slots without claiming that a tracked value reaches the callee; a default value is
     * modeled where it materializes in the callee body.
     */
    private fun IrFunctionAccessExpression.alignedArgumentValues(): List<JsDataFlowIR.Node> =
        arguments.map { arg ->
            arg?.toValueNode() ?: JsDataFlowIR.Node.Const(JsDataFlowIR.Type.Dynamic)
        }
}

/**
 * Collects the structural and value-flow facts needed before graph nodes can be placed.
 *
 * The visitor records loops, relevant expressions, reaching definitions, return and throw
 * values, and flows that cross the current execution-unit boundary. It does not analyze nested
 * declarations as part of the enclosing unit; it only records captured inputs as escapes.
 */
private class ElementFinderVisitor(
    private val expressionValuesExtractor: ExpressionValuesExtractor,
) : IrVisitorVoid() {
    val expressions = mutableMapOf<IrExpression, IrLoop?>()
    val parentLoops = mutableMapOf<IrLoop, IrLoop?>()
    val variableValues = VariableValues()
    val returnValues = mutableListOf<IrExpression>()
    val thrownValues = mutableListOf<IrExpression>()

    /** Values owned by the current execution unit and read by a nested declaration. */
    val escapedValues = mutableSetOf<IrValueDeclaration>()

    /** Values assigned to declarations owned outside the current execution unit. */
    val escapedExpressions = mutableListOf<IrExpression>()

    private val suspendableExpressionStack = mutableListOf<IrSuspendableExpression>()
    private val loopStack = mutableListOf<IrLoop>()
    private val currentLoop: IrLoop?
        get() = loopStack.peek()

    /**
     * Finds inputs captured by a nested declaration without treating that declaration's own
     * expressions as part of the enclosing execution unit.
     */
    private val nestedDeclarationVisitor = object : IrVisitorVoid() {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitGetValue(expression: IrGetValue) {
            escapedValues += expression.symbol.owner
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        // The module builder analyzes this function separately; only its captures affect this unit.
        declaration.acceptChildrenVoid(nestedDeclarationVisitor)
    }

    override fun visitClass(declaration: IrClass) {
        // Local classes also have independent bodies whose captures escape this unit.
        declaration.acceptChildrenVoid(nestedDeclarationVisitor)
    }

    private fun assignValue(variable: IrValueDeclaration, value: IrExpression) {
        expressionValuesExtractor.forEachValue(value) {
            variableValues.add(variable, it)
        }
    }

    override fun visitExpression(expression: IrExpression) {
        when (expression) {
            is IrMemberAccessExpression<*>,
            is IrRawFunctionReference,
            is IrGetField,
            is IrGetObjectValue,
            is IrVararg,
            is IrConst,
            is IrTypeOperatorCall,
            is IrConstantPrimitive,
            is IrClassReference,
            is IrGetClass,
            is IrDynamicMemberExpression,
            is IrDynamicOperatorExpression,
            is IrFunctionExpression,
                -> expressions += expression to currentLoop
        }

        if (expression is IrReturnableBlock) {
            expressionValuesExtractor.returnableBlockValues[expression] = mutableListOf()
        }
        if (expression is IrSuspendableExpression) {
            suspendableExpressionStack.push(expression)
            expressionValuesExtractor.suspendableExpressionValues[expression] = mutableListOf()
        }
        if (expression is IrSuspensionPoint) {
            expressionValuesExtractor.suspendableExpressionValues[suspendableExpressionStack.peek()!!]!!.add(expression)
        }
        if (expression is IrLoop) {
            parentLoops[expression] = currentLoop
            loopStack.push(expression)
        }

        super.visitExpression(expression)

        when (expression) {
            is IrLoop -> loopStack.pop()
            is IrSuspendableExpression -> suspendableExpressionStack.pop()
        }
    }

    override fun visitSetField(expression: IrSetField) {
        expressions += expression to currentLoop
        super.visitSetField(expression)
    }

    override fun visitReturn(expression: IrReturn) {
        val returnableBlock = expression.returnTargetSymbol.owner as? IrReturnableBlock
        if (returnableBlock != null) {
            expressionValuesExtractor.returnableBlockValues[returnableBlock]!!.add(expression.value)
        } else if (!expression.type.isUnit()) {
            returnValues += expression.value
        }
        super.visitReturn(expression)
    }

    override fun visitThrow(expression: IrThrow) {
        thrownValues += expression.value
        super.visitThrow(expression)
    }

    override fun visitSetValue(expression: IrSetValue) {
        super.visitSetValue(expression)
        val target = expression.symbol.owner
        if (target in variableValues.elementData) {
            assignValue(target, expression.value)
        } else {
            // The graph cannot follow a value written into storage owned by another unit.
            escapedExpressions += expression.value
        }
    }

    override fun visitVariable(declaration: IrVariable) {
        variableValues.addEmpty(declaration, currentLoop)
        super.visitVariable(declaration)
        declaration.initializer?.let { assignValue(declaration, it) }
    }

    override fun visitConstantArray(expression: IrConstantArray) {
        super.visitConstantArray(expression)
        expressions += expression to currentLoop
    }

    override fun visitConstantObject(expression: IrConstantObject) {
        super.visitConstantObject(expression)
        expressions += expression to currentLoop
    }
}
