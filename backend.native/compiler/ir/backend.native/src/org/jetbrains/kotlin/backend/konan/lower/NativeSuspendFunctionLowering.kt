package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.descriptors.synthesizedName
import org.jetbrains.kotlin.backend.common.ir.isSuspend
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrVariableImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedVariableDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSetVariableImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSuspendableExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrSuspensionPointImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrVariableSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.Name

internal class NativeSuspendFunctionsLowering(ctx: Context): AbstractSuspendFunctionsLowering<Context>(ctx) {
    private val symbols = context.ir.symbols

    override val stateMachineMethodName = Name.identifier("invokeSuspend")

    override fun getCoroutineBaseClass(function: IrFunction): IrClassSymbol =
            (if (function.isRestrictedSuspendFunction(context.irBuiltIns.languageVersionSettings)) {
                symbols.restrictedContinuationImpl
            } else {
                symbols.continuationImpl
            })

    override fun nameForCoroutineClass(function: IrFunction) =
            "${function.name}COROUTINE\$${context.coroutineCount++}".synthesizedName

    override fun initializeStateMachine(coroutineConstructors: List<IrConstructor>, coroutineClassThis: IrValueDeclaration) {
        for (constructor in coroutineConstructors) {
            val labelField = constructor.parentAsClass.declarations.single { it is IrField && it.name.asString() == "label" } as IrField
            (constructor.body as IrBlockBody).statements += with(context.createIrBuilder(constructor.symbol, constructor.startOffset, constructor.endOffset)) {
                irSetField(irGet(coroutineClassThis), labelField, irCall(symbols.getNativeNullPtr.owner))
            }
        }
    }

    override fun IrBlockBodyBuilder.generateCoroutineStart(invokeSuspendFunction: IrFunction, receiver: IrExpression) {
        +irReturn(
                irCall(invokeSuspendFunction).apply {
                    dispatchReceiver = receiver
                    putValueArgument(0, irSuccess(irGetObject(symbols.unit)))
                }
        )
    }

    override fun buildStateMachine(stateMachineFunction: IrFunction,
                                   transformingFunction: IrFunction,
                                   argumentToPropertiesMap: Map<IrValueParameter, IrField>) {
        val originalBody = transformingFunction.body!!
        val resultArgument = stateMachineFunction.valueParameters.single()

        val coroutineClass = stateMachineFunction.parentAsClass

        val labelField = coroutineClass.addField(Name.identifier("label"), symbols.nativePtrType, true)

        val startOffset = transformingFunction.startOffset
        val endOffset = transformingFunction.endOffset

        val irBuilder = context.createIrBuilder(stateMachineFunction.symbol, startOffset, endOffset)
        stateMachineFunction.body = irBuilder.irBlockBody(startOffset, endOffset) {

            val suspendResult = irVar("suspendResult".synthesizedName, context.irBuiltIns.anyNType, true)

            // Extract all suspend calls to temporaries in order to make correct jumps to them.
            originalBody.transformChildrenVoid(ExpressionSlicer(labelField.type, transformingFunction, saveState, restoreState, suspendResult, resultArgument))

            val liveLocals = computeLivenessAtSuspensionPoints(originalBody)

            val immutableLiveLocals = liveLocals.values.flatten().filterNot { it.isVar }.toSet()
            val localsMap = immutableLiveLocals.associate { it to irVar(it.name, it.type, true) }

            if (localsMap.isNotEmpty())
                transformVariables(originalBody, localsMap)    // Make variables mutable in order to save/restore them.

            val localToPropertyMap = mutableMapOf<IrVariableSymbol, IrField>()
            // TODO: optimize by using the same property for different locals.
            liveLocals.values.forEach { scope ->
                scope.forEach {
                    localToPropertyMap.getOrPut(it.symbol) {
                        coroutineClass.addField(it.name, it.type, true)
                    }
                }
            }

            originalBody.transformChildrenVoid(object : IrElementTransformerVoid() {

                private val thisReceiver = stateMachineFunction.dispatchReceiverParameter!!

                // Replace returns to refer to the new function.
                override fun visitReturn(expression: IrReturn): IrExpression {
                    expression.transformChildrenVoid(this)

                    return if (expression.returnTargetSymbol != transformingFunction.symbol)
                        expression
                    else
                        irReturn(expression.value)
                }

                // Replace function arguments loading with properties reading.
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    expression.transformChildrenVoid(this)

                    val capturedValue = argumentToPropertiesMap[expression.symbol.owner]
                            ?: return expression
                    return irGetField(irGet(thisReceiver), capturedValue)
                }

                // Save/restore state at suspension points.
                override fun visitExpression(expression: IrExpression): IrExpression {
                    expression.transformChildrenVoid(this)

                    val suspensionPoint = expression as? IrSuspensionPoint
                            ?: return expression

                    suspensionPoint.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitCall(expression: IrCall): IrExpression {
                            expression.transformChildrenVoid(this)

                            when (expression.symbol) {
                                saveState.symbol -> {
                                    val scope = liveLocals[suspensionPoint]!!
                                    return irBlock(expression) {
                                        scope.forEach {
                                            val variable = localsMap[it] ?: it
                                            +irSetField(irGet(thisReceiver), localToPropertyMap[it.symbol]!!, irGet(variable))
                                        }
                                        +irSetField(
                                                irGet(thisReceiver),
                                                labelField,
                                                irGet(suspensionPoint.suspensionPointIdParameter)
                                        )
                                    }
                                }
                                restoreState.symbol -> {
                                    val scope = liveLocals[suspensionPoint]!!
                                    return irBlock(expression) {
                                        scope.forEach {
                                            +irSetVar(localsMap[it]
                                                    ?: it, irGetField(irGet(thisReceiver), localToPropertyMap[it.symbol]!!))
                                        }
                                    }
                                }
                            }
                            return expression
                        }
                    })

                    return suspensionPoint
                }
            })
            originalBody.setDeclarationsParent(stateMachineFunction)
            val statements = (originalBody as IrBlockBody).statements
            +suspendResult
            +IrSuspendableExpressionImpl(
                    startOffset, endOffset,
                    context.irBuiltIns.unitType,
                    suspensionPointId = irGetField(irGet(stateMachineFunction.dispatchReceiverParameter!!), labelField),
                    result = irBlock(startOffset, endOffset) {
                        +irThrowIfNotNull(irExceptionOrNull(irGet(resultArgument))) // Coroutine might start with an exception.
                        statements.forEach { +it }
                    })
            if (transformingFunction.returnType.isUnit())
                +irReturn(irGetObject(symbols.unit))                             // Insert explicit return for Unit functions.
        }
    }

    private var tempIndex = 0
    private var suspensionPointIdIndex = 0

    private fun transformVariables(element: IrElement, variablesMap: Map<IrVariable, IrVariable>) {
        element.transformChildrenVoid(object: IrElementTransformerVoid() {

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                expression.transformChildrenVoid(this)

                val newVariable = variablesMap[expression.symbol.owner]
                        ?: return expression

                return IrGetValueImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        type        = newVariable.type,
                        symbol      = newVariable.symbol,
                        origin      = expression.origin)
            }

            override fun visitSetVariable(expression: IrSetVariable): IrExpression {
                expression.transformChildrenVoid(this)

                val newVariable = variablesMap[expression.symbol.owner]
                        ?: return expression

                return IrSetVariableImpl(
                        startOffset = expression.startOffset,
                        endOffset   = expression.endOffset,
                        type        = context.irBuiltIns.unitType,
                        symbol      = newVariable.symbol,
                        value       = expression.value,
                        origin      = expression.origin)
            }

            override fun visitVariable(declaration: IrVariable): IrStatement {
                declaration.transformChildrenVoid(this)

                val newVariable = variablesMap[declaration]
                        ?: return declaration

                newVariable.initializer = declaration.initializer

                return newVariable
            }
        })
    }

    private fun computeLivenessAtSuspensionPoints(body: IrBody): Map<IrSuspensionPoint, List<IrVariable>> {
        // TODO: data flow analysis.
        // Just save all visible for now.
        val result = mutableMapOf<IrSuspensionPoint, List<IrVariable>>()
        body.acceptChildrenVoid(object: VariablesScopeTracker() {

            override fun visitExpression(expression: IrExpression) {
                val suspensionPoint = expression as? IrSuspensionPoint
                if (suspensionPoint == null) {
                    super.visitExpression(expression)
                    return
                }

                suspensionPoint.result.acceptChildrenVoid(this)
                suspensionPoint.resumeResult.acceptChildrenVoid(this)

                val visibleVariables = mutableListOf<IrVariable>()
                scopeStack.forEach { visibleVariables += it }
                result[suspensionPoint] = visibleVariables
            }
        })

        return result
    }

    private inner class ExpressionSlicer(val suspensionPointIdType: IrType, val irFunction: IrFunction,
                                         val saveState: IrFunction, val restoreState: IrFunction,
                                         val suspendResult: IrVariable, val resultArgument: IrValueParameter): IrElementTransformerVoid() {
        // TODO: optimize - it has square complexity.

        override fun visitSetField(expression: IrSetField): IrExpression {
            expression.transformChildrenVoid(this)

            return sliceExpression(expression)
        }

        override fun visitMemberAccess(expression: IrMemberAccessExpression<*>): IrExpression {
            expression.transformChildrenVoid(this)

            return sliceExpression(expression)
        }

        private fun sliceExpression(expression: IrExpression): IrExpression {
            val irBuilder = context.createIrBuilder(irFunction.symbol, expression.startOffset, expression.endOffset)
            irBuilder.run {
                val children = when (expression) {
                    is IrSetField -> listOf(expression.receiver, expression.value)
                    is IrMemberAccessExpression<*> -> (
                            listOf(expression.dispatchReceiver, expression.extensionReceiver)
                                    + (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it) }
                            )
                    else -> throw Error("Unexpected expression: $expression")
                }

                val numberOfChildren = children.size

                val hasSuspendCallInTail = BooleanArray(numberOfChildren + 1)
                for (i in numberOfChildren - 1 downTo 0)
                    hasSuspendCallInTail[i] = hasSuspendCallInTail[i + 1] || children[i].let { it != null && it.hasSuspendCalls() }

                val newChildren = arrayOfNulls<IrExpression?>(numberOfChildren)
                val tempStatements = mutableListOf<IrStatement>()
                var first = true
                for ((index, child) in children.withIndex()) {
                    if (child == null) continue
                    val transformedChild =
                            if (!child.isSpecialBlock())
                                child
                            else {
                                val statements = (child as IrBlock).statements
                                tempStatements += statements.take(statements.size - 1)
                                statements.last() as IrExpression
                            }
                    if (first && !hasSuspendCallInTail[index + 1]) {
                        // Don't extract suspend call to a temporary if it is the first argument and is the only suspend call.
                        newChildren[index] = transformedChild
                        first = false
                        continue
                    }
                    first = false
                    if (transformedChild.isPure() || !hasSuspendCallInTail[index])
                        newChildren[index] = transformedChild
                    else {
                        // Save to temporary in order to save execution order.
                        val tmp = irVar(transformedChild)

                        tempStatements += tmp
                        newChildren[index] = irGet(tmp)
                    }
                }

                var calledSaveState = false
                var suspendCall: IrExpression? = null
                when {
                    expression.isReturnIfSuspendedCall -> {
                        calledSaveState = true
                        val firstArgument = newChildren[2]!!
                        newChildren[2] = irBlock(firstArgument) {
                            +irCall(saveState)
                            +firstArgument
                        }
                        suspendCall = newChildren[2]
                    }
                    expression.isSuspendCall -> {
                        val lastChildIndex = newChildren.indexOfLast { it != null }
                        if (lastChildIndex != -1) {
                            // Save state as late as possible.
                            val lastChild = newChildren[lastChildIndex]!!
                            calledSaveState = true
                            newChildren[lastChildIndex] =
                                    irBlock(lastChild) {
                                        if (lastChild.isPure()) {
                                            +irCall(saveState)
                                            +lastChild
                                        } else {
                                            val tmp = irVar(lastChild)
                                            +tmp
                                            +irCall(saveState)
                                            +irGet(tmp)
                                        }
                                    }
                        }
                        suspendCall = expression
                    }
                }

                when (expression) {
                    is IrSetField -> {
                        expression.receiver = newChildren[0]
                        expression.value = newChildren[1]!!
                    }
                    is IrMemberAccessExpression<*> -> {
                        expression.dispatchReceiver = newChildren[0]
                        expression.extensionReceiver = newChildren[1]
                        newChildren.drop(2).forEachIndexed { index, newChild ->
                            expression.putValueArgument(index, newChild)
                        }
                    }
                }

                if (suspendCall == null)
                    return irWrap(expression, tempStatements)

                val suspensionPoint = IrSuspensionPointImpl(
                        startOffset                = startOffset,
                        endOffset                  = endOffset,
                        type                       = context.irBuiltIns.anyNType,
                        suspensionPointIdParameter = irVar(
                                "suspensionPointId${suspensionPointIdIndex++}".synthesizedName,
                                suspensionPointIdType
                        ),
                        result                     = irBlock(startOffset, endOffset) {
                            if (!calledSaveState)
                                +irCall(saveState)
                            +irSetVar(suspendResult.symbol, suspendCall)
                            +irReturnIfSuspended(suspendResult)
                            +irGet(suspendResult)
                        },
                        resumeResult               = irBlock(startOffset, endOffset) {
                            +irCall(restoreState)
                            +irGetOrThrow(irGet(resultArgument))
                        })
                val expressionResult = when {
                    suspendCall.type.isUnit() -> irImplicitCoercionToUnit(suspensionPoint)
                    else -> irAs(suspensionPoint, suspendCall.type)
                }
                return irBlock(expression) {
                    tempStatements.forEach { +it }
                    +expressionResult
                }
            }

        }
    }

    private fun IrBuilderWithScope.irWrap(expression: IrExpression, tempStatements: List<IrStatement>)
            = if (tempStatements.isEmpty())
        expression
    else irBlock(expression, STATEMENT_ORIGIN_COROUTINE_IMPL) {
        tempStatements.forEach { +it }
        +expression
    }


    private fun IrBuilderWithScope.irThrowIfNotNull(exception: IrExpression) = irLetS(exception) {
        irThrowIfNotNull(it.owner)
    }

    fun IrBuilderWithScope.irThrowIfNotNull(exception: IrValueDeclaration) =
            irIfThen(irNot(irEqeqeq(irGet(exception), irNull())),
                    irThrow(irImplicitCast(irGet(exception), exception.type.makeNotNull())))

    private val IrExpression.isSuspendCall: Boolean
        get() = this is IrCall && this.isSuspend

    private fun IrElement.isSpecialBlock()
            = this is IrBlock && this.origin == STATEMENT_ORIGIN_COROUTINE_IMPL

    private fun IrElement.hasSuspendCalls(): Boolean {
        var hasSuspendCalls = false
        acceptVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)
                hasSuspendCalls = hasSuspendCalls || expression.isSuspendCall
            }

            override fun visitExpression(expression: IrExpression) {
                expression.acceptChildrenVoid(this)
                hasSuspendCalls = hasSuspendCalls || expression is IrSuspensionPointImpl
            }
        })

        return hasSuspendCalls
    }

    private val IrExpression.isReturnIfSuspendedCall: Boolean
        get() = this is IrCall && isReturnIfSuspendedCall()

    private fun IrBuilderWithScope.irVar(initializer: IrExpression) =
            irVar("tmp${tempIndex++}".synthesizedName, initializer.type, false, initializer)


    private fun IrBuilderWithScope.irReturnIfSuspended(value: IrValueDeclaration) =
            irIfThen(irEqeqeq(irGet(value), irCall(symbols.coroutineSuspendedGetter)),
                    irReturn(irGet(value)))

    private fun IrExpression.isPure(): Boolean {
        return when (this) {
            is IrConst<*> -> true
            is IrCall -> false // TODO: skip builtin operators.
            is IrTypeOperatorCall -> this.argument.isPure() && this.operator != IrTypeOperator.CAST
            is IrGetValue -> !this.symbol.owner.let { it is IrVariable && it.isVar }
            else -> false
        }
    }

    // These are marker functions to split up the lowering on two parts.
    private val saveState = WrappedSimpleFunctionDescriptor().let {
        IrFunctionImpl(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                IrDeclarationOrigin.DEFINED,
                IrSimpleFunctionSymbolImpl(it),
                "saveState".synthesizedName,
                Visibilities.PRIVATE,
                Modality.ABSTRACT,
                context.irBuiltIns.unitType,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false,
                isInfix = false
        ).apply {
            it.bind(this)
        }
    }

    private val restoreState = WrappedSimpleFunctionDescriptor().let {
        IrFunctionImpl(
                SYNTHETIC_OFFSET, SYNTHETIC_OFFSET,
                IrDeclarationOrigin.DEFINED,
                IrSimpleFunctionSymbolImpl(it),
                "restoreState".synthesizedName,
                Visibilities.PRIVATE,
                Modality.ABSTRACT,
                context.irBuiltIns.unitType,
                isInline = false,
                isExternal = false,
                isTailrec = false,
                isSuspend = false,
                isExpect = false,
                isFakeOverride = false,
                isOperator = false,
                isInfix = false
        ).apply {
            it.bind(this)
        }
    }

    private fun IrBuilderWithScope.irVar(name: Name, type: IrType,
                                         isMutable: Boolean = false,
                                         initializer: IrExpression? = null) = WrappedVariableDescriptor().let {
        IrVariableImpl(
                startOffset, endOffset,
                DECLARATION_ORIGIN_COROUTINE_IMPL,
                IrVariableSymbolImpl(it),
                name,
                type,
                isMutable,
                isConst = false,
                isLateinit = false
        ).apply {
            it.bind(this)
            this.initializer = initializer
            this.parent = this@irVar.parent
        }
    }

    private fun IrBuilderWithScope.irGetOrThrow(result: IrExpression): IrExpression =
            irCall(symbols.kotlinResultGetOrThrow.owner).apply {
                extensionReceiver = result
            } // TODO: consider inlining getOrThrow function body here.

    private fun IrBuilderWithScope.irExceptionOrNull(result: IrExpression): IrExpression {
        val resultClass = symbols.kotlinResult.owner
        val exceptionOrNull = resultClass.simpleFunctions().single { it.name.asString() == "exceptionOrNull" }
        return irCall(exceptionOrNull).apply {
            dispatchReceiver = result
        }
    }

    fun IrBlockBodyBuilder.irSuccess(value: IrExpression): IrMemberAccessExpression<*> {
        val createResult = symbols.kotlinResult.owner.constructors.single { it.isPrimary }
        return irCall(createResult).apply {
            putValueArgument(0, value)
        }
    }
}