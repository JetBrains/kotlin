/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.interpreter.builtins.*
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterException
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterMethodNotFoundException
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterTimeOutException
import org.jetbrains.kotlin.ir.interpreter.intrinsics.IntrinsicEvaluator
import org.jetbrains.kotlin.ir.interpreter.stack.StackImpl
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.invoke.MethodHandle
import kotlin.concurrent.thread

private const val MAX_COMMANDS = 500_000

class IrInterpreter(private val irBuiltIns: IrBuiltIns, private val bodyMap: Map<IdSignature, IrBody> = emptyMap()) {
    private val irExceptions = mutableListOf<IrClass>()

    private val stack = StackImpl()
    private var commandCount = 0

    private val mapOfEnums = mutableMapOf<IrSymbol, Complex>()
    private val mapOfObjects = mutableMapOf<IrSymbol, Complex>()

    constructor(irModule: IrModuleFragment): this(irModule.irBuiltins) {
        irExceptions.addAll(
            irModule.files
                .flatMap { it.declarations }
                .filterIsInstance<IrClass>()
                .filter { it.isSubclassOf(irBuiltIns.throwableClass.owner) }
        )
    }

    private fun Any?.getType(defaultType: IrType): IrType {
        return when (this) {
            is Boolean -> irBuiltIns.booleanType
            is Char -> irBuiltIns.charType
            is Byte -> irBuiltIns.byteType
            is Short -> irBuiltIns.shortType
            is Int -> irBuiltIns.intType
            is Long -> irBuiltIns.longType
            is String -> irBuiltIns.stringType
            is Float -> irBuiltIns.floatType
            is Double -> irBuiltIns.doubleType
            null -> irBuiltIns.nothingNType
            else -> when (defaultType.classifierOrNull?.owner) {
                is IrTypeParameter -> stack.getVariable(defaultType.classifierOrFail).state.irClass.defaultType
                else -> defaultType
            }
        }
    }

    private fun incrementAndCheckCommands() {
        commandCount++
        if (commandCount >= MAX_COMMANDS) throw InterpreterTimeOutException()
    }

    fun interpret(expression: IrExpression): IrExpression {
        stack.clean()
        lateinit var result: IrExpression
        thread(start = true) {
            result = try {
                when (val returnLabel = expression.interpret().returnLabel) {
                    ReturnLabel.REGULAR -> stack.popReturnValue().toIrExpression(expression)
                    ReturnLabel.EXCEPTION -> {
                        val message = (stack.popReturnValue() as ExceptionState).getFullDescription()
                        IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, "\n" + message)
                    }
                    else -> TODO("$returnLabel not supported as result of interpretation")
                }
            } catch (e: InterpreterException) {
                // TODO don't handle, throw to lowering
                IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, "\n" + e.message)
            }
        }.join()
        return result
    }

    private fun IrElement.interpret(): ExecutionResult {
        try {
            incrementAndCheckCommands()
            val executionResult = when (this) {
                is IrSimpleFunction -> interpretFunction(this)
                is IrCall -> interpretCall(this)
                is IrConstructorCall -> interpretConstructorCall(this)
                is IrEnumConstructorCall -> interpretEnumConstructorCall(this)
                is IrDelegatingConstructorCall -> interpretDelegatedConstructorCall(this)
                is IrInstanceInitializerCall -> interpretInstanceInitializerCall(this)
                is IrBody -> interpretBody(this)
                is IrBlock -> interpretBlock(this)
                is IrReturn -> interpretReturn(this)
                is IrSetField -> interpretSetField(this)
                is IrGetField -> interpretGetField(this)
                is IrGetValue -> interpretGetValue(this)
                is IrGetObjectValue -> interpretGetObjectValue(this)
                is IrGetEnumValue -> interpretGetEnumValue(this)
                is IrEnumEntry -> interpretEnumEntry(this)
                is IrConst<*> -> interpretConst(this)
                is IrVariable -> interpretVariable(this)
                is IrSetValue -> interpretSetVariable(this)
                is IrTypeOperatorCall -> interpretTypeOperatorCall(this)
                is IrBranch -> interpretBranch(this)
                is IrWhileLoop -> interpretWhile(this)
                is IrDoWhileLoop -> interpretDoWhile(this)
                is IrWhen -> interpretWhen(this)
                is IrBreak -> interpretBreak(this)
                is IrContinue -> interpretContinue(this)
                is IrVararg -> interpretVararg(this)
                is IrSpreadElement -> interpretSpreadElement(this)
                is IrTry -> interpretTry(this)
                is IrCatch -> interpretCatch(this)
                is IrThrow -> interpretThrow(this)
                is IrStringConcatenation -> interpretStringConcatenation(this)
                is IrFunctionExpression -> interpretFunctionExpression(this)
                is IrFunctionReference -> interpretFunctionReference(this)
                is IrComposite -> interpretComposite(this)

                else -> TODO("${this.javaClass} not supported")
            }

            return executionResult.getNextLabel(this) { this@getNextLabel.interpret() }
        } catch (e: InterpreterException) {
            throw e
        } catch (e: Throwable) {
            // catch exception from JVM such as: ArithmeticException, StackOverflowError and others
            val exceptionName = e::class.java.simpleName
            val irExceptionClass = irExceptions.firstOrNull { it.name.asString() == exceptionName } ?: irBuiltIns.throwableClass.owner
            stack.pushReturnValue(ExceptionState(e, irExceptionClass, stack.getStackTrace()))
            return Exception
        }
    }

    // this method is used to get stack trace after exception
    private fun interpretFunction(irFunction: IrSimpleFunction): ExecutionResult {
        if (irFunction.fileOrNull != null) stack.setCurrentFrameName(irFunction)

        if (irFunction.body is IrSyntheticBody) return handleIntrinsicMethods(irFunction)
        return irFunction.body?.interpret() ?: throw InterpreterException("Ir function must be with body")
    }

    private fun MethodHandle?.invokeMethod(irFunction: IrFunction): ExecutionResult {
        this ?: return handleIntrinsicMethods(irFunction)
        val result = this.invokeWithArguments(irFunction.getArgsForMethodInvocation(stack.getAll()))
        stack.pushReturnValue(result.toState(result.getType(irFunction.returnType)))

        return Next
    }

    private fun handleIntrinsicMethods(irFunction: IrFunction): ExecutionResult {
        return IntrinsicEvaluator().evaluate(irFunction, stack) { this.interpret() }
    }

    private fun calculateBuiltIns(irFunction: IrFunction): ExecutionResult {
        val methodName = when (val property = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol) {
            null -> irFunction.name.asString()
            else -> property.owner.name.asString()
        }
        val args = stack.getAll().map { it.state }

        val receiverType = irFunction.dispatchReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + irFunction.valueParameters.map { it.type }
        val argsValues = args.map {
            when (it) {
                is Complex -> when (irFunction.fqNameWhenAvailable?.asString()) {
                    // must explicitly convert Common to String in String plus method or else will be taken default toString from Common
                    "kotlin.String.plus" -> stack.apply { interpretToString(it) }.popReturnValue().asString()
                    else -> it.getOriginal()
                }
                is Primitive<*> -> it.value
                is Lambda -> it // lambda can be used in built in calculation, for example, in null check or toString
                else -> TODO("unsupported type of argument for builtins calculations: ${it::class.java}")
            }
        }

        fun IrType.getOnlyName(): String {
            return when {
                this.originalKotlinType != null -> this.originalKotlinType.toString()
                this is IrSimpleType -> (this.classifierOrFail.owner as IrDeclarationWithName).name.asString() + (if (this.hasQuestionMark) "?" else "")
                else -> this.render()
            }
        }

        val signature = CompileTimeFunction(methodName, argsType.map { it.getOnlyName() })

        // TODO replace unary, binary, ternary functions with vararg
        val result = when (argsType.size) {
            1 -> {
                val function = unaryFunctions[signature]
                    ?: throw InterpreterMethodNotFoundException("For given function $signature there is no entry in unary map")
                function.invoke(argsValues.first())
            }
            2 -> {
                val function = binaryFunctions[signature]
                    ?: throw InterpreterMethodNotFoundException("For given function $signature there is no entry in binary map")
                when (methodName) {
                    "rangeTo" -> return calculateRangeTo(irFunction.returnType)
                    else -> function.invoke(argsValues[0], argsValues[1])
                }
            }
            3 -> {
                val function = ternaryFunctions[signature]
                    ?: throw InterpreterMethodNotFoundException("For given function $signature there is no entry in ternary map")
                function.invoke(argsValues[0], argsValues[1], argsValues[2])
            }
            else -> throw InterpreterException("Unsupported number of arguments")
        }

        stack.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
        return Next
    }

    private fun calculateRangeTo(type: IrType): ExecutionResult {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

        val primitiveValueParameters = stack.getAll().map { it.state as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(primitive.type))
        }

        val constructorValueParameters = constructor.valueParameters.map { it.symbol }.zip(primitiveValueParameters)
        return stack.newFrame(initPool = constructorValueParameters.map { Variable(it.first, it.second) }) {
            constructorCall.interpret()
        }
    }

    private fun interpretValueParameters(
        expression: IrFunctionAccessExpression, irFunction: IrFunction, pool: MutableList<Variable>
    ): ExecutionResult {
        // if irFunction is lambda and it has receiver, then first descriptor must be taken from extension receiver
        val receiverAsFirstArgument = when (expression.dispatchReceiver?.type?.isFunction()) {
            true -> listOfNotNull(irFunction.getExtensionReceiver())
            else -> listOf()
        }
        val valueParametersSymbols = receiverAsFirstArgument + irFunction.valueParameters.map { it.symbol }

        val valueArguments = (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it) }
        val defaultValues = expression.symbol.owner.valueParameters.map { it.defaultValue?.expression }

        return stack.newFrame(asSubFrame = true, initPool = pool) {
            for (i in valueArguments.indices) {
                (valueArguments[i] ?: defaultValues[i])?.interpret()?.check { return@newFrame it }
                    ?: stack.pushReturnValue(listOf<Any?>().toPrimitiveStateArray(expression.getVarargType(i)!!)) // if vararg is empty

                stack.peekReturnValue().checkNullability(valueParametersSymbols[i].owner.type) {
                    val method = irFunction.getCapitalizedFileName() + "." + irFunction.fqNameWhenAvailable
                    val parameter = valueParametersSymbols[i].owner.name
                    throw IllegalArgumentException("Parameter specified as non-null is null: method $method, parameter $parameter")
                }

                with(Variable(valueParametersSymbols[i], stack.popReturnValue())) {
                    stack.addVar(this)  //must add value argument in current stack because it can be used later as default argument
                    pool.add(this)
                }
            }
            Next
        }
    }

    private fun interpretCall(expression: IrCall): ExecutionResult {
        val valueArguments = mutableListOf<Variable>()
        // dispatch receiver processing
        val rawDispatchReceiver = expression.dispatchReceiver
        rawDispatchReceiver?.interpret()?.check { return it }
        val dispatchReceiver = rawDispatchReceiver?.let { stack.popReturnValue() }?.checkNullability(expression.dispatchReceiver?.type)

        // extension receiver processing
        val rawExtensionReceiver = expression.extensionReceiver
        rawExtensionReceiver?.interpret()?.check { return it }
        val extensionReceiver = rawExtensionReceiver?.let { stack.popReturnValue() }?.checkNullability(expression.extensionReceiver?.type)

        // get correct ir function
        val irFunction = dispatchReceiver?.getIrFunctionByIrCall(expression) ?: expression.symbol.owner
        val functionReceiver = dispatchReceiver.getCorrectReceiverByFunction(irFunction)

        // it is important firstly to add receiver, then arguments; this order is used in builtin method call
        irFunction.getDispatchReceiver()?.let { functionReceiver?.let { receiver -> valueArguments.add(Variable(it, receiver)) } }
        irFunction.getExtensionReceiver()?.let { extensionReceiver?.let { receiver -> valueArguments.add(Variable(it, receiver)) } }

        interpretValueParameters(expression, irFunction, valueArguments).check { return it }

        valueArguments.addAll(getTypeArguments(irFunction, expression) { stack.getVariable(it).state })
        if (dispatchReceiver is Complex) valueArguments.addAll(dispatchReceiver.typeArguments)
        if (extensionReceiver is Complex) valueArguments.addAll(extensionReceiver.typeArguments)

        val isLocal = (dispatchReceiver as? Complex)?.getOriginal()?.irClass?.isLocal ?: irFunction.isLocal
        if (isLocal) valueArguments.addAll(dispatchReceiver.extractNonLocalDeclarations())

        if (functionReceiver is Complex && irFunction.parentClassOrNull?.isInner == true) {
            generateSequence(functionReceiver.outerClass) { (it.state as? Complex)?.outerClass }.forEach { valueArguments.add(it) }
        }

        return stack.newFrame(asSubFrame = irFunction.isInline || irFunction.isLocal, initPool = valueArguments) {
            // inline only methods are not presented in lookup table, so must be interpreted instead of execution
            val isInlineOnly = irFunction.hasAnnotation(FqName("kotlin.internal.InlineOnly"))
            return@newFrame when {
                dispatchReceiver is Wrapper && !isInlineOnly -> dispatchReceiver.getMethod(irFunction).invokeMethod(irFunction)
                irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction)
                dispatchReceiver is Primitive<*> -> calculateBuiltIns(irFunction) // 'is Primitive' check for js char and js long
                irFunction.body == null ->
                    irFunction.trySubstituteFunctionBody() ?: irFunction.tryCalculateLazyConst() ?: calculateBuiltIns(irFunction)
                else -> irFunction.interpret()
            }
        }.check { return it }.implicitCastIfNeeded(expression.type, irFunction.returnType, stack)
    }

    private fun IrFunction.trySubstituteFunctionBody(): ExecutionResult? {
        val signature = this.symbol.signature ?: return null
        val body = bodyMap[signature]

        return body?.let {
            try {
                this.body = it
                this.interpret()
            } finally {
                this.body = null
            }
        }
    }

    // TODO fix in FIR2IR; const val getter must have body with IrGetField node
    private fun IrFunction.tryCalculateLazyConst(): ExecutionResult? {
        if (this !is IrSimpleFunction) return null
        return this.correspondingPropertySymbol?.owner?.backingField?.initializer?.interpret()
    }

    private fun interpretInstanceInitializerCall(call: IrInstanceInitializerCall): ExecutionResult {
        val irClass = call.classSymbol.owner

        // properties processing
        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        classProperties.forEach { property ->
            property.backingField?.initializer?.expression?.interpret()?.check { return it }
            val receiver = irClass.thisReceiver!!.symbol
            if (property.backingField?.initializer != null) {
                val receiverState = stack.getVariable(receiver).state
                val propertyVar = Variable(property.symbol, stack.popReturnValue())
                receiverState.setField(propertyVar)
            }
        }

        // init blocks processing
        val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }
        anonymousInitializer.forEach { init -> init.body.interpret().check { return it } }

        return Next
    }

    private fun interpretConstructor(constructorCall: IrFunctionAccessExpression): ExecutionResult {
        val owner = constructorCall.symbol.owner
        val valueArguments = mutableListOf<Variable>()

        interpretValueParameters(constructorCall, owner, valueArguments).check { return it }

        val irClass = owner.parent as IrClass
        val typeArguments = getTypeArguments(irClass, constructorCall) { stack.getVariable(it).state }
        if (irClass.hasAnnotation(evaluateIntrinsicAnnotation) || irClass.fqNameWhenAvailable!!.startsWith(Name.identifier("java"))) {
            return stack.newFrame(initPool = valueArguments) { Wrapper.getConstructorMethod(owner).invokeMethod(owner) }
                .apply { stack.peekReturnValue().addTypeArguments(typeArguments) }
        }

        if (irClass.defaultType.isArray() || irClass.defaultType.isPrimitiveArray()) {
            // array constructor doesn't have body so must be treated separately
            return stack.newFrame(initPool = valueArguments) { handleIntrinsicMethods(owner) }
                .apply { stack.peekReturnValue().addTypeArguments(typeArguments) }
        }

        val state = Common(irClass).apply { this.addTypeArguments(typeArguments) }
        if (irClass.isLocal) state.fields.addAll(stack.getAll()) // TODO save only necessary declarations
        if (irClass.isInner) {
            constructorCall.dispatchReceiver!!.interpret().check { return it }
            state.outerClass = Variable(irClass.parentAsClass.thisReceiver!!.symbol, stack.popReturnValue())
        }
        valueArguments.add(Variable(irClass.thisReceiver!!.symbol, state)) //used to set up fields in body
        return stack.newFrame(initPool = valueArguments + state.typeArguments) {
            val statements = constructorCall.getBody()!!.statements
            // enum entry use IrTypeOperatorCall with IMPLICIT_COERCION_TO_UNIT as delegation call, but we need the value
            ((statements[0] as? IrTypeOperatorCall)?.argument ?: statements[0]).interpret().check { return@newFrame it }
            val returnedState = stack.popReturnValue() as Complex

            for (i in 1 until statements.size) statements[i].interpret().check { return@newFrame it }

            stack.pushReturnValue(state.apply { this.setSuperClassInstance(returnedState) })
            Next
        }
    }

    private fun interpretConstructorCall(constructorCall: IrConstructorCall): ExecutionResult {
        return interpretConstructor(constructorCall)
    }

    private fun interpretEnumConstructorCall(enumConstructorCall: IrEnumConstructorCall): ExecutionResult {
        return interpretConstructor(enumConstructorCall)
    }

    private fun interpretDelegatedConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall): ExecutionResult {
        if (delegatingConstructorCall.symbol.owner.parent == irBuiltIns.anyClass.owner) {
            val anyAsStateObject = Common(irBuiltIns.anyClass.owner)
            stack.pushReturnValue(anyAsStateObject)
            return Next
        }

        return interpretConstructor(delegatingConstructorCall)
    }

    private fun interpretConst(expression: IrConst<*>): ExecutionResult {
        fun getSignedType(unsignedType: IrType): IrType {
            return when {
                unsignedType.isUByte() -> irBuiltIns.byteType
                unsignedType.isUShort() -> irBuiltIns.shortType
                unsignedType.isUInt() -> irBuiltIns.intType
                unsignedType.isULong() -> irBuiltIns.longType
                else -> throw InterpreterException("Unsupported unsigned class ${unsignedType.render()}")
            }
        }

        return if (expression.type.isUnsigned()) {
            val unsignedClass = expression.type.classOrNull!!
            val constructor = unsignedClass.constructors.single().owner
            val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
            constructorCall.putValueArgument(0, expression.value.toIrConst(getSignedType(expression.type)))

            constructorCall.interpret()
        } else {
            stack.pushReturnValue(expression.toPrimitive())
            Next
        }
    }

    private fun interpretStatements(statements: List<IrStatement>): ExecutionResult {
        var executionResult: ExecutionResult = Next
        for (statement in statements) {
            when (statement) {
                is IrClass -> if (statement.isLocal) Next else TODO("Only local classes are supported")
                is IrFunction -> if (statement.isLocal) Next else TODO("Only local functions are supported")
                else -> executionResult = statement.interpret().check { return it }
            }
        }
        return executionResult
    }

    private fun interpretBlock(block: IrBlock): ExecutionResult {
        return stack.newFrame(asSubFrame = true) { interpretStatements(block.statements) }
    }

    private fun interpretBody(body: IrBody): ExecutionResult {
        return stack.newFrame(asSubFrame = true) { interpretStatements(body.statements) }
    }

    private fun interpretReturn(expression: IrReturn): ExecutionResult {
        expression.value.interpret().check { return it }
        return Return.addOwnerInfo(expression.returnTargetSymbol.owner)
    }

    private fun interpretWhile(expression: IrWhileLoop): ExecutionResult {
        while (true) {
            expression.condition.interpret().check { return it }
            if (stack.popReturnValue().asBooleanOrNull() != true) break
            expression.body?.interpret()?.check { return it }
        }
        return Next
    }

    private fun interpretDoWhile(expression: IrDoWhileLoop): ExecutionResult {
        do {
            // pool from body must be seen to condition expression, so must create temp frame here
            stack.newFrame(asSubFrame = true) {
                expression.body?.interpret()?.check { return@newFrame it }
                expression.condition.interpret().check { return@newFrame it }
                Next
            }.check { return it }

            if (stack.popReturnValue().asBooleanOrNull() != true) break
        } while (true)
        return Next
    }

    private fun interpretWhen(expression: IrWhen): ExecutionResult {
        var executionResult: ExecutionResult = Next
        for (branch in expression.branches) {
            executionResult = branch.interpret().check { return it }
        }
        return executionResult
    }

    private fun interpretBranch(expression: IrBranch): ExecutionResult {
        val executionResult = expression.condition.interpret().check { return it }
        if (stack.popReturnValue().asBooleanOrNull() == true) {
            expression.result.interpret().check { return it }
            return BreakWhen
        }
        return executionResult
    }

    private fun interpretBreak(breakStatement: IrBreak): ExecutionResult {
        return BreakLoop.addOwnerInfo(breakStatement.loop)
    }

    private fun interpretContinue(continueStatement: IrContinue): ExecutionResult {
        return Continue.addOwnerInfo(continueStatement.loop)
    }

    private fun interpretSetField(expression: IrSetField): ExecutionResult {
        expression.value.interpret().check { return it }

        // receiver is null only for top level var, but it cannot be used in constexpr; corresponding check is on frontend
        val receiver = (expression.receiver as IrDeclarationReference).symbol
        val propertySymbol = expression.symbol.owner.correspondingPropertySymbol!!
        stack.getVariable(receiver).apply { this.state.setField(Variable(propertySymbol, stack.popReturnValue())) }
        return Next
    }

    private fun interpretGetField(expression: IrGetField): ExecutionResult {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol
        val field = expression.symbol.owner
        // for java static variables
        if (field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && field.isStatic) {
            val initializerExpression = field.initializer?.expression
            if (initializerExpression is IrConst<*>) {
                return interpretConst(initializerExpression)
            }
            stack.pushReturnValue(Wrapper.getStaticGetter(field)!!.invokeWithArguments().toState(field.type))
            return Next
        }
        // receiver is null, for example, for top level fields
        val result = receiver?.let { stack.getVariable(receiver).state.getState(field.correspondingPropertySymbol!!) }
            ?: return (expression.symbol.owner.initializer?.expression?.interpret() ?: Next)
        stack.pushReturnValue(result)
        return Next
    }

    private fun interpretGetValue(expression: IrGetValue): ExecutionResult {
        val owner = expression.type.classOrNull?.owner
        // used to evaluate constants inside object
        if (owner != null && owner.isObject) return getOrCreateObjectValue(owner) // TODO is this correct behaviour?
        stack.pushReturnValue(stack.getVariable(expression.symbol).state)
        return Next
    }

    private fun interpretVariable(expression: IrVariable): ExecutionResult {
        expression.initializer?.interpret()?.check { return it } ?: return Next
        stack.addVar(Variable(expression.symbol, stack.popReturnValue()))
        return Next
    }

    private fun interpretSetVariable(expression: IrSetValue): ExecutionResult {
        expression.value.interpret().check { return it }

        if (stack.contains(expression.symbol)) {
            stack.getVariable(expression.symbol).apply { this.state = stack.popReturnValue() }
        } else {
            stack.addVar(Variable(expression.symbol, stack.popReturnValue()))
        }
        return Next
    }

    private fun interpretGetObjectValue(expression: IrGetObjectValue): ExecutionResult {
        return getOrCreateObjectValue(expression.symbol.owner)
    }

    private fun getOrCreateObjectValue(objectClass: IrClass): ExecutionResult {
        mapOfObjects[objectClass.symbol]?.let { return Next.apply { stack.pushReturnValue(it) } }

        val objectState = when {
            objectClass.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getCompanionObject(objectClass)
            else -> Common(objectClass).apply { setSuperClassRecursive() } // TODO test type arguments
        }
        mapOfObjects[objectClass.symbol] = objectState
        stack.pushReturnValue(objectState)
        return Next
    }

    private fun interpretGetEnumValue(expression: IrGetEnumValue): ExecutionResult {
        mapOfEnums[expression.symbol]?.let { return Next.apply { stack.pushReturnValue(it) } }

        val enumEntry = expression.symbol.owner
        val enumClass = enumEntry.symbol.owner.parentAsClass
        val valueOfFun = enumClass.declarations.single { it.nameForIrSerialization.asString() == "valueOf" } as IrFunction
        enumClass.declarations.filterIsInstance<IrEnumEntry>().forEach {
            val executionResult = when {
                enumClass.hasAnnotation(evaluateIntrinsicAnnotation) -> {
                    val enumEntryName = it.name.asString().toState(irBuiltIns.stringType)
                    val enumNameAsVariable = Variable(valueOfFun.valueParameters.first().symbol, enumEntryName)
                    stack.newFrame(initPool = listOf(enumNameAsVariable)) { Wrapper.getEnumEntry(enumClass)!!.invokeMethod(valueOfFun) }
                }
                else -> interpretEnumEntry(it)
            }
            executionResult.check { result -> return result }
            mapOfEnums[it.symbol] = stack.popReturnValue() as Complex
        }

        stack.pushReturnValue(mapOfEnums[expression.symbol]!!)
        return Next
    }

    private fun interpretEnumEntry(enumEntry: IrEnumEntry): ExecutionResult {
        val enumClass = enumEntry.symbol.owner.parentAsClass
        val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()

        val enumSuperCall = (enumClass.primaryConstructor?.body?.statements?.firstOrNull() as? IrEnumConstructorCall)
        if (enumEntries.isNotEmpty() && enumSuperCall != null) {
            val valueArguments = listOf(
                enumEntry.name.asString().toIrConst(irBuiltIns.stringType), enumEntries.indexOf(enumEntry).toIrConst(irBuiltIns.intType)
            )
            valueArguments.forEachIndexed { index, irConst -> enumSuperCall.putValueArgument(index, irConst) }
        }

        val executionResult = enumEntry.initializerExpression?.interpret()?.check { return it }
        enumSuperCall?.apply { (0 until this.valueArgumentsCount).forEach { putValueArgument(it, null) } } // restore to null
        return executionResult ?: throw InterpreterException("Initializer at enum entry ${enumEntry.fqNameWhenAvailable} is null")
    }

    private fun interpretTypeOperatorCall(expression: IrTypeOperatorCall): ExecutionResult {
        val executionResult = expression.argument.interpret().check { return it }
        val typeClassifier = expression.typeOperand.classifierOrFail
        val isReified = (typeClassifier.owner as? IrTypeParameter)?.isReified == true
        val isErased = typeClassifier.owner is IrTypeParameter && !isReified
        val typeOperand = if (isReified) stack.getVariable(typeClassifier).state.irClass.defaultType else expression.typeOperand

        when (expression.operator) {
            // coercion to unit means that return value isn't used
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> stack.popReturnValue()
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                if (!isErased && !stack.peekReturnValue().isSubtypeOf(typeOperand)) {
                    val convertibleClassName = stack.popReturnValue().irClass.fqNameWhenAvailable
                    throw ClassCastException("$convertibleClassName cannot be cast to ${typeOperand.render()}")
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                if (!isErased && !stack.peekReturnValue().isSubtypeOf(typeOperand)) {
                    stack.popReturnValue()
                    stack.pushReturnValue(null.toState(irBuiltIns.nothingNType))
                }
            }
            IrTypeOperator.INSTANCEOF -> {
                val isInstance = isErased || stack.peekReturnValue().isSubtypeOf(typeOperand)
                stack.pushReturnValue(isInstance.toState(irBuiltIns.nothingType))
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val isInstance = isErased || stack.peekReturnValue().isSubtypeOf(typeOperand)
                stack.pushReturnValue((!isInstance).toState(irBuiltIns.nothingType))
            }
            IrTypeOperator.IMPLICIT_NOTNULL -> {

            }
            else -> TODO("${expression.operator} not implemented")
        }
        return executionResult
    }

    private fun interpretVararg(expression: IrVararg): ExecutionResult {
        val args = expression.elements.flatMap {
            it.interpret().check { executionResult -> return executionResult }
            return@flatMap when (val result = stack.popReturnValue()) {
                is Wrapper -> listOf(result.value)
                is Primitive<*> ->
                    when (val value = result.value) {
                        is ByteArray -> value.toList()
                        is CharArray -> value.toList()
                        is ShortArray -> value.toList()
                        is IntArray -> value.toList()
                        is LongArray -> value.toList()
                        is FloatArray -> value.toList()
                        is DoubleArray -> value.toList()
                        is BooleanArray -> value.toList()
                        is Array<*> -> value.toList()
                        else -> listOf(value)
                    }
                else -> listOf(result)
            }
        }

        val array = when ((expression.type.classifierOrFail.owner as? IrDeclaration)?.nameForIrSerialization?.asString()) {
            "UByteArray", "UShortArray", "UIntArray", "ULongArray" -> {
                val owner = expression.type.classOrNull!!.owner
                val storageProperty = owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "storage" }
                val primitiveArray = args.map { ((it as Common).fields.single().state as Primitive<*>).value }
                val unsignedArray = primitiveArray.toPrimitiveStateArray(storageProperty.backingField!!.type)
                Common(owner).apply {
                    setSuperClassRecursive()
                    fields.add(Variable(storageProperty.symbol, unsignedArray))
                }
            }
            else -> args.toPrimitiveStateArray(expression.type)
        }
        stack.pushReturnValue(array)
        return Next
    }

    private fun interpretSpreadElement(spreadElement: IrSpreadElement): ExecutionResult {
        return spreadElement.expression.interpret().check { return it }
    }

    private fun interpretTry(expression: IrTry): ExecutionResult {
        try {
            expression.tryResult.interpret().check(ReturnLabel.EXCEPTION) { return it } // if not exception -> return
            val exception = stack.peekReturnValue() as ExceptionState
            for (catchBlock in expression.catches) {
                if (exception.isSubtypeOf(catchBlock.catchParameter.type.classOrNull!!.owner)) {
                    catchBlock.interpret().check { return it }
                    break
                }
            }
        } finally {
            expression.finallyExpression?.interpret()?.check { return it }
        }

        return Next
    }

    private fun interpretCatch(expression: IrCatch): ExecutionResult {
        val catchParameter = Variable(expression.catchParameter.symbol, stack.popReturnValue())
        return stack.newFrame(asSubFrame = true, initPool = listOf(catchParameter)) {
            expression.result.interpret()
        }
    }

    private fun interpretThrow(expression: IrThrow): ExecutionResult {
        expression.value.interpret().check { return it }
        when (val exception = stack.popReturnValue()) {
            is Common -> stack.pushReturnValue(ExceptionState(exception, stack.getStackTrace()))
            is Wrapper -> stack.pushReturnValue(ExceptionState(exception, stack.getStackTrace()))
            is ExceptionState -> stack.pushReturnValue(exception)
            else -> throw InterpreterException("${exception::class} cannot be used as exception state")
        }
        return Exception
    }

    private fun interpretStringConcatenation(expression: IrStringConcatenation): ExecutionResult {
        val result = StringBuilder()
        expression.arguments.forEach {
            it.interpret().check { executionResult -> return executionResult }
            interpretToString(stack.popReturnValue()).check { executionResult -> return executionResult }
            result.append(stack.popReturnValue().asString())
        }

        stack.pushReturnValue(result.toString().toState(expression.type))
        return Next
    }

    private fun interpretToString(state: State): ExecutionResult {
        val result = when (state) {
            is Primitive<*> -> state.value.toString()
            is Wrapper -> state.value.toString()
            is Common -> {
                val toStringFun = state.getToStringFunction()
                return stack.newFrame(initPool = mutableListOf(Variable(toStringFun.getReceiver()!!, state))) {
                    toStringFun.body?.let { toStringFun.interpret() } ?: calculateBuiltIns(toStringFun)
                }
            }
            is Lambda -> state.toString()
            else -> throw InterpreterException("${state::class.java} cannot be used in StringConcatenation expression")
        }
        stack.pushReturnValue(result.toState(irBuiltIns.stringType))
        return Next
    }

    private fun interpretFunctionExpression(expression: IrFunctionExpression): ExecutionResult {
        val lambda = Lambda(expression.function, expression.type.classOrNull!!.owner)
        if (expression.function.isLocal) lambda.fields.addAll(stack.getAll()) // TODO save only necessary declarations
        stack.pushReturnValue(lambda)
        return Next
    }

    private fun interpretFunctionReference(reference: IrFunctionReference): ExecutionResult {
        stack.pushReturnValue(Lambda(reference.symbol.owner, reference.type.classOrNull!!.owner))
        return Next
    }

    private fun interpretComposite(expression: IrComposite): ExecutionResult {
        return when (expression.origin) {
            IrStatementOrigin.DESTRUCTURING_DECLARATION -> interpretStatements(expression.statements)
            null -> interpretStatements(expression.statements) // is null for body of do while loop
            else -> TODO("${expression.origin} not implemented")
        }
    }
}