/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.backend.common.compilationException
import org.jetbrains.kotlin.backend.common.lower.BOUND_VALUE_PARAMETER
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrFileEntry
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.JsLoweredDeclarationOrigin
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.lower.*
import org.jetbrains.kotlin.ir.backend.js.sourceMapsInfo
import org.jetbrains.kotlin.ir.backend.js.utils.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.SideEffectKind
import org.jetbrains.kotlin.js.backend.ast.metadata.isGeneratorFunction
import org.jetbrains.kotlin.js.backend.ast.metadata.sideEffects
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.js.config.SourceMapNamesPolicy
import org.jetbrains.kotlin.js.config.SourceMapSourceEmbedding
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import org.jetbrains.kotlin.utils.memoryOptimizedMap
import org.jetbrains.kotlin.utils.memoryOptimizedPlus
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

fun jsUndefined(context: JsStaticContext): JsExpression {
    return when (val void = context.backendContext.getVoid()) {
        is IrGetField -> context.getNameForField(void.symbol.owner).makeRef()
        else -> JsNullLiteral()
    }
}

fun <T : JsNode> IrWhen.toJsNode(
    tr: BaseIrElementToJsNodeTransformer<T, JsGenerationContext>,
    context: JsGenerationContext,
    node: (JsExpression, T, T?) -> T,
    implicitElse: T? = null,
): T? =
    branches.foldRight(implicitElse) { br, n ->
        val body = br.result.accept(tr, context)
        if (isElseBranch(br)) body
        else {
            val condition = br.condition.accept(IrElementToJsExpressionTransformer(), context)
            node(condition, body, n).withSource(br, context)
        }
    }

fun jsElementAccess(name: String, receiver: JsExpression?): JsExpression =
    jsElementAccess(JsName(name, false), receiver)

fun JsExpression.putIntoVariableWitName(name: JsName): JsVars {
    return JsVars(JsVars.JsVar(name, this))
}

fun jsElementAccess(name: JsName, receiver: JsExpression?): JsExpression =
    if (receiver == null || name.ident.isValidES5Identifier()) {
        JsNameRef(name, receiver)
    } else {
        JsArrayAccess(receiver, JsStringLiteral(name.ident))
    }

fun jsAssignment(left: JsExpression, right: JsExpression) = JsBinaryOperation(JsBinaryOperator.ASG, left, right)

fun prototypeOf(classNameRef: JsExpression, context: JsStaticContext) =
    JsInvocation(
        context
            .getNameForStaticFunction(context.backendContext.intrinsics.jsPrototypeOfSymbol.owner)
            .makeRef(),
        classNameRef
    )

fun objectCreate(prototype: JsExpression, context: JsStaticContext) =
    JsInvocation(
        context
            .getNameForStaticFunction(context.backendContext.intrinsics.jsObjectCreateSymbol.owner)
            .makeRef(),
        prototype
    )

fun defineProperty(obj: JsExpression, name: String, getter: JsExpression?, setter: JsExpression?, context: JsStaticContext): JsExpression {
    return JsInvocation(
        context
            .getNameForStaticFunction(context.backendContext.intrinsics.jsDefinePropertySymbol.owner)
            .makeRef(),
        listOfNotNull(obj, JsStringLiteral(name), getter ?: runIf(setter != null) { jsUndefined(context) }, setter)
    )
}

fun translateFunction(declaration: IrFunction, name: JsName?, context: JsGenerationContext): JsFunction {
    context.staticContext.backendContext.getJsCodeForFunction(declaration.symbol)?.let { function ->
        function.name = name
        return function
    }

    val localNameGenerator = context.localNames
        ?: LocalNameGenerator(context.staticContext.globalNameScope).also {
            declaration.acceptChildrenVoid(it)
            declaration.parentClassOrNull?.thisReceiver?.acceptVoid(it)
        }

    val functionContext = context.newDeclaration(declaration, localNameGenerator)

    val functionParams = declaration.valueParameters.map { it to functionContext.getNameForValueDeclaration(it) }
    val body = declaration.body?.accept(IrElementToJsStatementTransformer(), functionContext) as? JsBlock ?: JsBlock()

    val function = JsFunction(emptyScope, body, "member function ${name ?: "annon"}")
        .apply {
            if (declaration.isEs6ConstructorReplacement) modifiers.add(JsFunction.Modifier.STATIC)
            if (declaration.shouldBeCompiledAsGenerator()) {
                name?.isGeneratorFunction = true
                modifiers.add(JsFunction.Modifier.GENERATOR)
            }
        }
        .withSource(declaration, context, useNameOf = declaration)

    function.name = name

    fun JsFunction.addParameter(parameter: JsName, irValueParameter: IrValueParameter) {
        parameters.add(JsParameter(parameter).withSource(irValueParameter, functionContext, useNameOf = irValueParameter))
    }

    declaration.extensionReceiverParameter?.let { function.addParameter(functionContext.getNameForValueDeclaration(it), it) }
    functionParams.forEach { (irValueParameter, name) -> function.addParameter(name, irValueParameter) }
    check(!declaration.isSuspend) { "All Suspend functions should be lowered" }

    return function
}

private fun IrFunction.shouldBeCompiledAsGenerator(): Boolean =
    hasAnnotation(JsAnnotations.jsGeneratorFqn)

private fun isFunctionTypeInvoke(receiver: JsExpression?, call: IrCall): Boolean {
    if (receiver == null || receiver is JsThisRef) return false
    val simpleFunction = call.symbol.owner
    val receiverType = simpleFunction.dispatchReceiverParameter?.type ?: return false

    if (call.origin === JsStatementOrigins.EXPLICIT_INVOKE) return false

    val isInvokeFun = simpleFunction.name == OperatorNameConventions.INVOKE
    if (!isInvokeFun) return false

    val isNonSuspendFunction = receiverType.isFunctionTypeOrSubtype() && !receiverType.isSuspendFunctionTypeOrSubtype()
    val isSuspendFunction = receiverType.isSuspendFunction()

    // Dce can eliminate Function parent of SuspendFunctionN
    // So we need to check them separately
    return isNonSuspendFunction || isSuspendFunction
}

fun translateCall(
    expression: IrCall,
    context: JsGenerationContext,
    transformer: IrElementToJsExpressionTransformer,
): JsExpression {
    val function = expression.symbol.owner.realOverrideTarget
    val currentDispatchReceiver = context.currentFunction?.parentClassOrNull

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
            val impl = function.resolveFakeOverrideOrFail()
            Pair(impl, impl.parentAsClass)
        } else {
            Pair(function, superQualifier.owner)
        }

        if (currentDispatchReceiver.canUseSuperRef(context, klass)) {
            return JsInvocation(JsNameRef(context.getNameForMemberFunction(target), JsSuperRef()), arguments)
        }

        val callRef = if (klass.isInterface) {
            val nameForStaticDeclaration = context.getNameForStaticDeclaration(target)
            JsNameRef(Namer.CALL_FUNCTION, JsNameRef(nameForStaticDeclaration))
        } else {
            val qualifierName = klass.getClassRef(context.staticContext)
            val targetName = context.getNameForMemberFunction(target)
            val qPrototype = JsNameRef(targetName, prototypeOf(qualifierName, context.staticContext))
            JsNameRef(Namer.CALL_FUNCTION, qPrototype)
        }

        return JsInvocation(callRef, jsDispatchReceiver?.let { receiver -> listOf(receiver) memoryOptimizedPlus arguments } ?: arguments)
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

    if (symbolName.isGeneratorFunction) {
        (ref.commentsBeforeNode ?: mutableListOf<JsComment>().also { ref.commentsBeforeNode = it })
            .add(JsMultiLineComment("#__NOINLINE__"))
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
        JsInvocation(ref, listOfNotNull(jsExtensionReceiver) memoryOptimizedPlus arguments).pureIfPossible(function, context)
    }
}

private fun JsInvocation.pureIfPossible(function: IrFunction, context: JsGenerationContext) = apply {
    if (function.symbol.isUnitInstanceFunction(context.staticContext.backendContext)) {
        sideEffects = SideEffectKind.PURE
        qualifier.sideEffects = SideEffectKind.PURE
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
    allowDropTailVoids: Boolean = true,
): List<JsExpression> {
    val size = expression.valueArgumentsCount

    val function = expression.symbol.owner
    val varargParameterIndex = function.realOverrideTarget.varargParameterIndex()

    val validWithNullArgs = expression.validWithNullArgs()
    val jsUndefined by lazy(LazyThreadSafetyMode.NONE) { jsUndefined(context.staticContext) }

    val arguments = (0 until size)
        .mapIndexedTo(ArrayList(size)) { i, _ ->
            expression.getValueArgument(i).checkOnNullability(validWithNullArgs || function.valueParameters[i].isBoxParameter)
        }
        .mapIndexed { i, it ->
            val jsArgument = when {
                allowDropTailVoids && (it == null || it.isVoidGetter(context)) -> null
                else -> it?.accept(transformer, context)
            }

            val isEmptyExternalVararg = validWithNullArgs &&
                    varargParameterIndex == i &&
                    jsArgument is JsArrayLiteral &&
                    jsArgument.expressions.isEmpty()

            jsArgument.takeIf { !isEmptyExternalVararg || i != size - 1 }
        }
        .dropLastWhile { it == null }
        .memoryOptimizedMap { it ?: jsUndefined }

    check(!expression.symbol.isSuspend) { "Suspend functions should be lowered" }
    return arguments
}

private fun IrExpression.isVoidGetter(context: JsGenerationContext): Boolean = this is IrGetField &&
        symbol.owner.correspondingPropertySymbol == context.staticContext.backendContext.intrinsics.void


private fun IrExpression?.checkOnNullability(validWithNullArgs: Boolean) =
    also {
        if (it == null) {
            assert(validWithNullArgs)
        }
    }

private fun IrMemberAccessExpression<*>.validWithNullArgs() =
    this is IrFunctionAccessExpression && symbol.owner.isExternalOrInheritedFromExternal()

fun JsStatement.asBlock() = this as? JsBlock ?: JsBlock(this)

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
        elseStatement: JsStatement? = null,
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

internal fun <T : JsNode> T.withSource(
    node: IrElement,
    context: JsGenerationContext,
    useNameOf: IrDeclarationWithName? = null,
    container: IrDeclaration? = null,
): T {
    addSourceInfoIfNeed(node, context, useNameOf, container)
    return this
}

@Suppress("NOTHING_TO_INLINE")
private inline fun <T : JsNode> T.addSourceInfoIfNeed(
    node: IrElement,
    context: JsGenerationContext,
    useNameOf: IrDeclarationWithName?,
    container: IrDeclaration?,
) {
    val sourceMapsInfo = context.staticContext.backendContext.sourceMapsInfo ?: return
    val originalName = useNameOf?.originalNameForUseInSourceMap(sourceMapsInfo.namesPolicy)
    val location = context.getStartLocationForIrElement(node, originalName) ?: return
    val isNodeFromCurrentModule = context.currentFile.module.descriptor == context.staticContext.backendContext.module

    // TODO maybe it's better to fix in JsExpressionStatement
    val locationTarget = if (this is JsExpressionStatement) this.expression else this

    if (locationTarget is JsBlock && (node is IrBlockBody || node is IrBlock)) {
        locationTarget.closingBraceSource = if (container is IrConstructor) {
            // This is a hack. Without this special case, the closing brace in the generated code for constructors would always be mapped
            // to the closing brace of the Kotlin class declaration.
            context.getStartLocationForIrElement(node)
        } else {
            context.getEndLocationForIrElement(node)?.run {
                // Assuming that endOffset for IrBlock and IrBlockBody points to the character after the closing brace.
                // TODO: This doesn't produce good results if the node originates from an expression body
                // (meaning, in the source code; not to be confused with IrExpressionBody)
                if (startChar > 0) copy(startChar = startChar - 1) else null
            }
        }
    }

    locationTarget.source = when (sourceMapsInfo.sourceMapContentEmbedding) {
        SourceMapSourceEmbedding.NEVER -> location
        SourceMapSourceEmbedding.INLINING -> if (isNodeFromCurrentModule) location else location.withEmbeddedSource(context)
        SourceMapSourceEmbedding.ALWAYS -> location.withEmbeddedSource(context)
    }
}

private fun JsLocation.withEmbeddedSource(
    @Suppress("UNUSED_PARAMETER")
    context: JsGenerationContext,
): JsLocationWithEmbeddedSource {
    // FIXME: fileIdentity is used to distinguish between different files with the same paths.
    // For now we use the file's path to read its content, which makes fileIdentity useless.
    // However, when we have a mechanism to reliably get the source code from an IrFile or IrFileEntry no matter what's stored
    // in fileEntry.name (including the source code for external libraries or klibs with relative paths in them).
    // Another issue is that JS AST serializer/deserializer ignores fileIdentity, which means that this will not work with incremental
    // compilation.
    return JsLocationWithEmbeddedSource(this, fileIdentity = null /*context.currentFile.fileEntry*/) {
        try {
            InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
        } catch (e: IOException) {
            // TODO: If the source file is not available at path (e. g. it's an stdlib file), use heuristics to find it.
            // If all heuristics fail, use dumpKotlinLike() on freshly deserialized IrFile.
            null
        }
    }
}

fun IrElement.getStartSourceLocation(container: IrDeclaration): JsLocation? {
    val fileEntry = container.fileOrNull?.fileEntry ?: return null
    return getStartSourceLocation(fileEntry)
}

fun IrElement.getStartSourceLocation(fileEntry: IrFileEntry) =
    getSourceLocation(fileEntry) { startOffset }

inline fun IrElement.getSourceLocation(fileEntry: IrFileEntry, offsetSelector: IrElement.() -> Int): JsLocation? {
    if (startOffset == UNDEFINED_OFFSET || endOffset == UNDEFINED_OFFSET) return null
    val path = fileEntry.name
    val offset = offsetSelector()
    val (startLine, startColumn) = fileEntry.getLineAndColumnNumbers(offset)
    return JsLocation(path, startLine, startColumn)
}

/**
 * Returns a name of the original Kotlin declaration, or null, if it is a compiler generated declaration.
 */
private fun IrDeclarationWithName.originalNameForUseInSourceMap(policy: SourceMapNamesPolicy): String? {
    if (policy == SourceMapNamesPolicy.NO) return null
    when (this) {
        is IrField -> correspondingPropertySymbol?.let {
            return it.owner.originalNameForUseInSourceMap(policy)
        }

        is IrFunction -> if (policy == SourceMapNamesPolicy.FULLY_QUALIFIED_NAMES) {
            fqNameWhenAvailable?.let {
                return it.asString()
            }
        }

        is IrValueDeclaration -> if (origin !in nameMappingOriginAllowList) {
            return null
        }
    }
    return name.asString()
}

private val nameMappingOriginAllowList = setOf(
    IrDeclarationOrigin.DEFINED,
    IrDeclarationOrigin.FOR_LOOP_VARIABLE,
    IrDeclarationOrigin.CATCH_PARAMETER,
    IrDeclarationOrigin.CONTINUATION,
    BOUND_VALUE_PARAMETER,
    JsLoweredDeclarationOrigin.JS_SHADOWED_DEFAULT_PARAMETER,
)

private fun IrClass?.canUseSuperRef(context: JsGenerationContext, superClass: IrClass): Boolean {
    val currentFunction = context.currentFunction ?: return false
    return this != null &&
            context.staticContext.backendContext.es6mode &&
            !superClass.isInterface &&
            !isInner && !isLocal && !currentFunction.isEs6ConstructorReplacement && currentFunction.parentClassOrNull?.superClass?.symbol != context.staticContext.backendContext.coroutineSymbols.coroutineImpl
}