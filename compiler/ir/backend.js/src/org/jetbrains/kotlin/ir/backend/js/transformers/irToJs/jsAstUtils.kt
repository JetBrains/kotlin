/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.ir.isElseBranch
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull

fun jsVar(name: JsName, initializer: IrExpression?, context: JsGenerationContext): JsVars {
    val jsInitializer = initializer?.accept(IrElementToJsExpressionTransformer(), context)
    return JsVars(JsVars.JsVar(name, jsInitializer))
}

fun <T : JsNode> IrWhen.toJsNode(
    tr: BaseIrElementToJsNodeTransformer<T, JsGenerationContext>,
    data: JsGenerationContext,
    node: (JsExpression, T, T?) -> T,
    implicitElse: T? = null
): T? =
    branches.foldRight(implicitElse) { br, n ->
        val body = br.result.accept(tr, data)
        if (isElseBranch(br)) body
        else {
            val condition = br.condition.accept(IrElementToJsExpressionTransformer(), data)
            node(condition, body, n)
        }
    }

fun jsElementAccess(name: String, receiver: JsExpression?): JsExpression =
    if (receiver == null || name.isValidES5Identifier()) {
        JsNameRef(JsName(name, false), receiver)
    } else {
        JsArrayAccess(receiver, JsStringLiteral(name))
    }

fun jsAssignment(left: JsExpression, right: JsExpression) = JsBinaryOperation(JsBinaryOperator.ASG, left, right)

fun prototypeOf(classNameRef: JsExpression) = JsNameRef(Namer.PROTOTYPE_NAME, classNameRef)

fun translateFunction(declaration: IrFunction, name: JsName?, context: JsGenerationContext): JsFunction {
    val jsFun = declaration.getJsFunAnnotation()
    if (jsFun != null) {
        // JsFun internal annotation must have string containing valid function expression
        val function = (parseJsCode(jsFun)!!.single() as JsExpressionStatement).expression as JsFunction
        function.name = name
        return function
    }

    val localNameGenerator = context.localNames
        ?: LocalNameGenerator(context.staticContext.globalNameScope).also {
            declaration.acceptChildrenVoid(it)
            declaration.parentClassOrNull?.thisReceiver?.acceptVoid(it)
        }

    val functionContext = context.newDeclaration(declaration, localNameGenerator)

    val functionParams = declaration.valueParameters.map { functionContext.getNameForValueDeclaration(it) }
    val body = declaration.body?.accept(IrElementToJsStatementTransformer(), functionContext) as? JsBlock ?: JsBlock()

    val function = JsFunction(emptyScope, body, "member function ${name ?: "annon"}")

    function.name = name

    fun JsFunction.addParameter(parameter: JsName) {
        parameters.add(JsParameter(parameter))
    }

    declaration.extensionReceiverParameter?.let { function.addParameter(functionContext.getNameForValueDeclaration(it)) }
    functionParams.forEach { function.addParameter(it) }
    check(!declaration.isSuspend) { "All Suspend functions should be lowered" }

    return function
}

private fun isFunctionTypeInvoke(receiver: JsExpression?, call: IrCall): Boolean {
    if (receiver == null || receiver is JsThisRef) return false
    val simpleFunction = call.symbol.owner as? IrSimpleFunction ?: return false
    val receiverType = simpleFunction.dispatchReceiverParameter?.type ?: return false

    if (call.origin === JsStatementOrigins.EXPLICIT_INVOKE) return false

    return simpleFunction.name == OperatorNameConventions.INVOKE
            && receiverType.isFunctionTypeOrSubtype()
            && (!receiverType.isSuspendFunctionTypeOrSubtype() || receiverType.isSuspendFunction())
}

fun translateCall(
    expression: IrCall,
    context: JsGenerationContext,
    transformer: IrElementToJsExpressionTransformer
): JsExpression {
    val function = expression.symbol.owner.realOverrideTarget

    context.staticContext.intrinsics[function.symbol]?.let {
        return it(expression, context)
    }

    val jsDispatchReceiver = expression.dispatchReceiver?.accept(transformer, context)
    val jsExtensionReceiver = expression.extensionReceiver?.accept(transformer, context)
    val arguments = translateCallArguments(expression, context, transformer)

    // Transform external and interface's property accessor call
    // @JsName-annotated external and interface's property accessors are translated as function calls
    if (function.getJsName() == null) {
        val property = function.correspondingPropertySymbol?.owner
        if (
            property != null &&
            (property.isEffectivelyExternal() || property.isExportedMember(context.staticContext.backendContext))
        ) {
            if (function.overriddenSymbols.isEmpty() || function.overriddenStableProperty(context.staticContext.backendContext)) {
                val propertyName = context.getNameForProperty(property)
                val nameRef = when (jsDispatchReceiver) {
                    null -> JsNameRef(propertyName)
                    else -> jsElementAccess(propertyName.ident, jsDispatchReceiver)
                }
                return when (function) {
                    property.getter -> nameRef
                    property.setter -> jsAssignment(nameRef, arguments.single())
                    else -> compilationException(
                        "Function must be an accessor of corresponding property",
                        function
                    )
                }
            }
        }
    }

    if (isFunctionTypeInvoke(jsDispatchReceiver, expression) || expression.symbol.owner.isJsNativeInvoke()) {
        return JsInvocation(jsDispatchReceiver ?: jsExtensionReceiver!!, arguments)
    }

    expression.superQualifierSymbol?.let { superQualifier ->
        val (target: IrSimpleFunction, klass: IrClass) = if (superQualifier.owner.isInterface) {
            val impl = function.resolveFakeOverride()!!
            Pair(impl, impl.parentAsClass)
        } else {
            Pair(function, superQualifier.owner)
        }

        val callRef = if (klass.isInterface && target.body != null) {
            JsNameRef(Namer.CALL_FUNCTION, JsNameRef(context.getNameForStaticDeclaration(target)))
        } else {
            val qualifierName = context.getNameForClass(klass).makeRef()
            val targetName = context.getNameForMemberFunction(target)
            val qPrototype = JsNameRef(targetName, prototypeOf(qualifierName))
            JsNameRef(Namer.CALL_FUNCTION, qPrototype)
        }

        return JsInvocation(callRef, jsDispatchReceiver?.let { receiver -> listOf(receiver) + arguments } ?: arguments)
    }

    val varargParameterIndex = function.varargParameterIndex()
    val isExternalVararg = function.isEffectivelyExternal() && varargParameterIndex != -1

    val symbolName = when (jsDispatchReceiver) {
        null -> context.getNameForStaticFunction(function)
        else -> context.getNameForMemberFunction(function)
    }

    val ref = when (jsDispatchReceiver) {
        null -> JsNameRef(symbolName)
        else -> jsElementAccess(symbolName.ident, jsDispatchReceiver)
    }

    return if (isExternalVararg) {
        // TODO: Don't use `Function.prototype.apply` when number of arguments is known at compile time (e.g. there are no spread operators)

        val argumentsAsSingleArray = argumentsWithVarargAsSingleArray(
            expression,
            context,
            jsExtensionReceiver,
            arguments,
            varargParameterIndex
        )

        if (jsDispatchReceiver != null) {
            if (argumentsAsSingleArray is JsArrayLiteral) {
                JsInvocation(
                    jsElementAccess(symbolName.ident, jsDispatchReceiver),
                    argumentsAsSingleArray.expressions
                )
            } else {
                // TODO: Do not create IIFE at all? (Currently there is no reliable way to create temporary variable in current scope)
                val receiverName = JsName("\$externalVarargReceiverTmp", false)
                val receiverRef = receiverName.makeRef()

                val iifeFun = JsFunction(
                    emptyScope,
                    JsBlock(
                        JsVars(JsVars.JsVar(receiverName, jsDispatchReceiver)),
                        JsReturn(
                            JsInvocation(
                                JsNameRef("apply", jsElementAccess(symbolName.ident, receiverRef)),
                                listOf(
                                    receiverRef,
                                    argumentsAsSingleArray
                                )
                            )
                        )
                    ),
                    "VarargIIFE"
                )

                JsInvocation(
                    // Create scope for temporary variable holding dispatch receiver
                    // It is used both during method reference and passing `this` value to `apply` function.
                    JsNameRef(
                        "call",
                        iifeFun
                    ),
                    JsThisRef()
                )
            }
        } else {
            if (argumentsAsSingleArray is JsArrayLiteral) {
                JsInvocation(
                    JsNameRef(symbolName),
                    argumentsAsSingleArray.expressions
                )
            } else {
                JsInvocation(
                    JsNameRef("apply", JsNameRef(symbolName)),
                    listOf(JsNullLiteral(), argumentsAsSingleArray)
                )
            }
        }
    } else {
        JsInvocation(ref, listOfNotNull(jsExtensionReceiver) + arguments)
    }
}

fun argumentsWithVarargAsSingleArray(
    expression: IrFunctionAccessExpression,
    context: JsGenerationContext,
    additionalReceiver: JsExpression?,
    arguments: List<JsExpression>,
    varargParameterIndex: Int,
): JsExpression {
    // External vararg arguments should be represented in JS as multiple "plain" arguments (opposed to arrays in Kotlin)
    // We are using `Function.prototype.apply` function to pass all arguments as a single array.
    // For this purpose are concatenating non-vararg arguments with vararg.
    var arraysForConcat = mutableListOf<JsExpression>()
    arraysForConcat.addIfNotNull(additionalReceiver)

    val concatElements = mutableListOf<JsExpression>()

    arguments
        .forEachIndexed { index, argument ->
            when (index) {

                // Call `Array.prototype.slice` on vararg arguments in order to convert array-like objects into proper arrays
                varargParameterIndex -> {
                    val valueArgument = expression.getValueArgument(varargParameterIndex)

                    if (arraysForConcat.isNotEmpty()) {
                        concatElements.add(JsArrayLiteral(arraysForConcat))
                    }
                    arraysForConcat = mutableListOf()

                    val varargArgument = when (argument) {
                        is JsArrayLiteral -> argument
                        is JsNew -> argument.arguments.firstOrNull() as? JsArrayLiteral
                        else -> null
                    } ?: if (valueArgument is IrCall && valueArgument.symbol == context.staticContext.backendContext.intrinsics.arrayConcat)
                        argument
                    else
                        JsInvocation(JsNameRef("call", JsNameRef("slice", JsArrayLiteral())), argument)

                    concatElements.add(varargArgument)
                }

                else -> {
                    arraysForConcat.add(argument)
                }
            }
        }

    if (arraysForConcat.isNotEmpty()) {
        concatElements.add(JsArrayLiteral(arraysForConcat))
    }

    if (concatElements.isEmpty()) {
        return JsArrayLiteral()
    }

    if (concatElements.all { it is JsArrayLiteral }) {
        return concatElements
            .fold(mutableListOf<JsExpression>()) { aggregatedArrayExpressions, arrayLiteral ->
                arrayLiteral as JsArrayLiteral
                aggregatedArrayExpressions.addAll(arrayLiteral.expressions)
                aggregatedArrayExpressions
            }
            .let { JsArrayLiteral(it) }
    }

    return when (concatElements.size) {
        1 -> concatElements[0]
        else -> JsInvocation(
            JsNameRef("concat", concatElements.first()),
            concatElements.drop(1)
        )
    }
}

/**
 * Returns the index of the vararg parameter of the function if there is one, otherwise returns -1.
 */
fun IrFunction.varargParameterIndex() = valueParameters.indexOfFirst { it.varargElementType != null }

fun translateCallArguments(
    expression: IrMemberAccessExpression<IrFunctionSymbol>,
    context: JsGenerationContext,
    transformer: IrElementToJsExpressionTransformer,
): List<JsExpression> {
    val size = expression.valueArgumentsCount

    val varargParameterIndex = expression.symbol.owner.realOverrideTarget.varargParameterIndex()

    val validWithNullArgs = expression.validWithNullArgs()
    val arguments = (0 until size)
        .mapTo(ArrayList(size)) { index ->
            val argument = expression.getValueArgument(index)
            argument?.accept(transformer, context)
        }
        .onEach { result ->
            if (result == null) {
                assert(validWithNullArgs)
            }
        }
        .mapIndexed { index, result ->
            val isEmptyExternalVararg = validWithNullArgs &&
                    varargParameterIndex == index &&
                    result is JsArrayLiteral &&
                    result.expressions.isEmpty()

            if (isEmptyExternalVararg && index == size - 1) {
                null
            } else result
        }
        .dropLastWhile { it == null }
        .map { it ?: JsPrefixOperation(JsUnaryOperator.VOID, JsIntLiteral(1)) }

    check(!expression.symbol.isSuspend) { "Suspend functions should be lowered" }
    return arguments
}

private fun IrMemberAccessExpression<*>.validWithNullArgs() =
    this is IrFunctionAccessExpression && symbol.owner.isExternalOrInheritedFromExternal()

fun JsStatement.asBlock() = this as? JsBlock ?: JsBlock(this)

fun defineProperty(receiver: JsExpression, name: String, value: () -> JsExpression): JsInvocation {
    val objectDefineProperty = JsNameRef("defineProperty", Namer.JS_OBJECT)
    return JsInvocation(objectDefineProperty, receiver, JsStringLiteral(name), value())
}

fun defineProperty(receiver: JsExpression, name: String, getter: JsExpression?, setter: JsExpression? = null) =
    defineProperty(receiver, name) {
        JsObjectLiteral(true).apply {
            propertyInitializers += JsPropertyInitializer(JsStringLiteral("configurable"), JsBooleanLiteral(true))
            if (getter != null)
                propertyInitializers += JsPropertyInitializer(JsStringLiteral("get"), getter)
            if (setter != null)
                propertyInitializers += JsPropertyInitializer(JsStringLiteral("set"), setter)
        }
    }


// Partially copied from org.jetbrains.kotlin.js.translate.utils.JsAstUtils
object JsAstUtils {
    private fun deBlockIfPossible(statement: JsStatement): JsStatement {
        return if (statement is JsBlock && statement.statements.size == 1) {
            statement.statements[0]
        } else {
            statement
        }
    }

    fun newJsIf(
        ifExpression: JsExpression,
        thenStatement: JsStatement,
        elseStatement: JsStatement? = null
    ): JsIf {
        return JsIf(ifExpression, deBlockIfPossible(thenStatement), elseStatement?.let { deBlockIfPossible(it) })
    }

    fun and(op1: JsExpression, op2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.AND, op1, op2)
    }

    fun or(op1: JsExpression, op2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.OR, op1, op2)
    }

    fun equality(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.REF_EQ, arg1, arg2)
    }

    fun inequality(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.REF_NEQ, arg1, arg2)
    }

    fun lessThanEq(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.LTE, arg1, arg2)
    }

    fun lessThan(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.LT, arg1, arg2)
    }

    fun greaterThan(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.GT, arg1, arg2)
    }

    fun greaterThanEq(arg1: JsExpression, arg2: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.GTE, arg1, arg2)
    }

    fun assignment(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ASG, left, right)
    }

    fun assignmentToThisField(fieldName: String, right: JsExpression): JsStatement {
        return assignment(JsNameRef(fieldName, JsThisRef()), right).source(right.source).makeStmt()
    }

    fun decomposeAssignment(expr: JsExpression): Pair<JsExpression, JsExpression>? {
        if (expr !is JsBinaryOperation) return null

        return if (expr.operator != JsBinaryOperator.ASG) null else Pair(expr.arg1, expr.arg2)

    }

    fun sum(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ADD, left, right)
    }

    fun addAssign(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.ASG_ADD, left, right)
    }

    fun subtract(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.SUB, left, right)
    }

    fun mul(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.MUL, left, right)
    }

    fun div(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.DIV, left, right)
    }

    fun mod(left: JsExpression, right: JsExpression): JsBinaryOperation {
        return JsBinaryOperation(JsBinaryOperator.MOD, left, right)
    }

    fun not(expression: JsExpression): JsPrefixOperation {
        return JsPrefixOperation(JsUnaryOperator.NOT, expression)
    }

    fun typeOfIs(expression: JsExpression, string: JsStringLiteral): JsBinaryOperation {
        return equality(JsPrefixOperation(JsUnaryOperator.TYPEOF, expression), string)
    }

    fun newVar(name: JsName, expr: JsExpression?): JsVars {
        return JsVars(JsVars.JsVar(name, expr))
    }
}

internal fun <T : JsNode> T.withSource(node: IrElement, context: JsGenerationContext): T {
    addSourceInfoIfNeed(node, context)
    return this
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : JsNode> T.addSourceInfoIfNeed(node: IrElement, context: JsGenerationContext) {
    if (!context.staticContext.genSourcemaps) return

    if (node.startOffset == UNDEFINED_OFFSET || node.endOffset == UNDEFINED_OFFSET) return

    val fileEntry = context.currentFile.fileEntry

    val path = fileEntry.name
    val startLine = fileEntry.getLineNumber(node.startOffset)
    val startColumn = fileEntry.getColumnNumber(node.startOffset)

    // TODO maybe it's better to fix in JsExpressionStatement
    val locationTarget = if (this is JsExpressionStatement) this.expression else this
    locationTarget.source = JsLocation(path, startLine, startColumn)
}
