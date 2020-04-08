/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import kotlinx.coroutines.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.*
import org.jetbrains.kotlin.backend.common.interpreter.exceptions.InterpreterException
import org.jetbrains.kotlin.backend.common.interpreter.exceptions.InterpreterTimeOutException
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.backend.common.interpreter.state.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedTypes
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import java.lang.invoke.MethodHandle

private const val MAX_STACK_SIZE = 10_000
private const val MAX_COMMANDS = 500_000

class IrInterpreter(irModule: IrModuleFragment) {
    private val irBuiltIns = irModule.irBuiltins
    private val irExceptions = irModule.files.flatMap { it.declarations }.filterIsInstance<IrClass>()
        .filter { it.isSubclassOf(irBuiltIns.throwableClass.owner) }
    private val classCastException = irExceptions.first { it.name.asString() == ClassCastException::class.java.simpleName }
    private val illegalArgumentException = irExceptions.first { it.name.asString() == IllegalArgumentException::class.java.simpleName }

    private val stackTrace = mutableListOf<String>()
    private var commandCount = 0

    private val mapOfEnums = mutableMapOf<Pair<IrClass, String>, Complex>()

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
            null -> irBuiltIns.nothingType
            else -> defaultType
        }
    }

    private fun incrementAndCheckCommands() {
        commandCount++
        if (commandCount >= MAX_COMMANDS) throw InterpreterTimeOutException()
    }

    fun interpret(expression: IrExpression): IrExpression {
        val data = InterpreterFrame()
        return try {
            runBlocking {
                return@runBlocking when (val returnLabel = withContext(this.coroutineContext) { expression.interpret(data).returnLabel }) {
                    ReturnLabel.NEXT -> data.popReturnValue().toIrExpression(expression)
                    ReturnLabel.EXCEPTION -> {
                        val message = (data.popReturnValue() as ExceptionState).getFullDescription()
                        IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, "\n" + message)
                    }
                    else -> TODO("$returnLabel not supported as result of interpretation")
                }
            }
        } catch (e: InterpreterException) {
            // TODO don't handle, throw to lowering
            IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, "\n" + e.message)
        }
    }

    private suspend fun IrElement.interpret(data: Frame): ExecutionResult {
        try {
            incrementAndCheckCommands()
            val executionResult = when (this) {
                is IrFunctionImpl -> interpretFunction(this, data)
                is IrCall -> interpretCall(this, data)
                is IrConstructorCall -> interpretConstructorCall(this, data)
                is IrEnumConstructorCall -> interpretEnumConstructorCall(this, data)
                is IrDelegatingConstructorCall -> interpretDelegatedConstructorCall(this, data)
                is IrInstanceInitializerCall -> interpretInstanceInitializerCall(this, data)
                is IrBody -> interpretBody(this, data)
                is IrBlock -> interpretBlock(this, data)
                is IrReturn -> interpretReturn(this, data)
                is IrSetField -> interpretSetField(this, data)
                is IrGetField -> interpretGetField(this, data)
                is IrGetValue -> interpretGetValue(this, data)
                is IrGetObjectValue -> interpretGetObjectValue(this, data)
                is IrGetEnumValue -> interpretGetEnumValue(this, data)
                is IrEnumEntry -> interpretEnumEntry(this, data)
                is IrConst<*> -> interpretConst(this, data)
                is IrVariable -> interpretVariable(this, data)
                is IrSetVariable -> interpretSetVariable(this, data)
                is IrTypeOperatorCall -> interpretTypeOperatorCall(this, data)
                is IrBranch -> interpretBranch(this, data)
                is IrWhileLoop -> interpretWhile(this, data)
                is IrWhen -> interpretWhen(this, data)
                is IrBreak -> interpretBreak(this, data)
                is IrContinue -> interpretContinue(this, data)
                is IrVararg -> interpretVararg(this, data)
                is IrSpreadElement -> interpretSpreadElement(this, data)
                is IrTry -> interpretTry(this, data)
                is IrCatch -> interpretCatch(this, data)
                is IrThrow -> interpretThrow(this, data)
                is IrStringConcatenation -> interpretStringConcatenation(this, data)
                is IrFunctionExpression -> interpretFunctionExpression(this, data)
                is IrFunctionReference -> interpretFunctionReference(this, data)
                is IrComposite -> interpretComposite(this, data)

                else -> TODO("${this.javaClass} not supported")
            }

            return executionResult.getNextLabel(this, data) { this@getNextLabel.interpret(it) }
        } catch (e: InterpreterException) {
            throw e
        } catch (e: Throwable) {
            // catch exception from JVM such as: ArithmeticException, StackOverflowError and others
            val exceptionName = e::class.java.simpleName
            val irExceptionClass = irExceptions.firstOrNull { it.name.asString() == exceptionName } ?: irBuiltIns.throwableClass.owner

            val exceptionState = when {
                // this block is used to replace jvm null pointer exception with common one
                // TODO figure something better
                irExceptionClass == irBuiltIns.throwableClass.owner && exceptionName == "KotlinNullPointerException" ->
                    ExceptionState(e, irExceptions.first { it.name.asString() == "NullPointerException" }, stackTrace)
                else -> ExceptionState(e, irExceptionClass, stackTrace)
            }

            data.pushReturnValue(exceptionState)
            return Exception
        }
    }

    // this method is used to get stack trace after exception
    private suspend fun interpretFunction(irFunction: IrFunctionImpl, data: Frame): ExecutionResult {
        return try {
            yield()

            if (irFunction.fileOrNull != null) {
                val fileName = irFunction.file.name
                val lineNum = irFunction.fileEntry.getLineNumber(irFunction.startOffset) + 1
                stackTrace += "at ${fileName.replace(".kt", "Kt").capitalize()}.${irFunction.fqNameForIrSerialization}($fileName:$lineNum)"
            }

            if (stackTrace.size == MAX_STACK_SIZE) {
                throw StackOverflowError("")
            }

            when (val kind = (irFunction.body as? IrSyntheticBody)?.kind) {
                IrSyntheticBodyKind.ENUM_VALUES -> handleIntrinsicMethods(irFunction, data)
                IrSyntheticBodyKind.ENUM_VALUEOF -> handleIntrinsicMethods(irFunction, data)
                null -> irFunction.body?.interpret(data) ?: throw AssertionError("Ir function must be with body")
                else -> throw AssertionError("Unsupported IrSyntheticBodyKind $kind")
            }
        } finally {
            if (irFunction.fileOrNull != null) stackTrace.removeAt(stackTrace.lastIndex)
        }
    }

    private suspend fun MethodHandle?.invokeMethod(irFunction: IrFunction, data: Frame): ExecutionResult {
        this ?: return handleIntrinsicMethods(irFunction, data)
        val result = this.invokeWithArguments(irFunction.getArgsForMethodInvocation(data.getAll()))
        data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))

        return Next
    }

    private suspend fun handleIntrinsicMethods(irFunction: IrFunction, data: Frame): ExecutionResult {
        when (irFunction.name.asString()) {
            "emptyArray" -> {
                val result = emptyArray<Any?>()
                data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
            }
            "arrayOf" -> {
                val result = irFunction.getArgsForMethodInvocation(data.getAll()).toTypedArray()
                data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
            }
            "arrayOfNulls" -> {
                val size = (data.getVariableState(irFunction.valueParameters.first().descriptor) as Primitive<*>).value as Int
                val result = arrayOfNulls<Any?>(size)
                data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
            }
            "values", "enumValues" -> {
                val enumClass =
                    (irFunction.parent as? IrClass) ?: data.getVariableState(irFunction.typeParameters.first().descriptor).irClass
                val enumEntries = enumClass.declarations
                    .filterIsInstance<IrEnumEntry>()
                    .map { entry ->
                        entry.interpret(data).check { return it }
                        data.popReturnValue() as Common
                    }
                data.pushReturnValue(enumEntries.toTypedArray().toState(irBuiltIns.arrayClass.defaultType))
            }
            "valueOf", "enumValueOf" -> {
                val enumClass =
                    (irFunction.parent as? IrClass) ?: data.getVariableState(irFunction.typeParameters.first().descriptor).irClass
                val enumEntryName = (data.getVariableState(irFunction.valueParameters.first().descriptor) as Primitive<*>).value.toString()
                val enumEntry = enumClass.declarations
                    .filterIsInstance<IrEnumEntry>()
                    .singleOrNull { it.name.asString() == enumEntryName }
                if (enumEntry == null) {
                    val message = "No enum constant ${enumClass.fqNameForIrSerialization}.$enumEntryName"
                    data.pushReturnValue(ExceptionState(IllegalArgumentException(message), illegalArgumentException, stackTrace))
                    return Exception
                } else {
                    enumEntry.interpret(data).check { return it }
                }
            }
            "replace" -> {
                val states = data.getAll().map { it.state }
                val regex = states.filterIsInstance<Wrapper>().single().value as Regex
                val input = states.filterIsInstance<Primitive<*>>().single().value.toString()
                val transform = states.filterIsInstance<Lambda>().single().irFunction
                val matchResultParameter = transform.valueParameters.single()
                val result = regex.replace(input) {
                    val itAsState = Variable(matchResultParameter.descriptor, Wrapper(it, matchResultParameter.type.classOrNull!!.owner))
                    val newFrame = InterpreterFrame(mutableListOf(itAsState))
                    runBlocking { transform.interpret(newFrame) }
                    (newFrame.popReturnValue() as Primitive<*>).value.toString()
                }
                data.pushReturnValue(result.toState(irBuiltIns.stringType))
            }
            "hashCode" -> {
                if (irFunction.parentAsClass.isEnumClass) {
                    calculateBuiltIns(irFunction.getLastOverridden() as IrSimpleFunction, data)
                } else {
                    throw AssertionError("Hash code function intrinsic is supported only for enum class")
                }
            }
            else -> throw AssertionError("Unsupported intrinsic ${irFunction.name}")
        }

        return Next
    }

    private suspend fun calculateAbstract(irFunction: IrFunction, data: Frame): ExecutionResult {
        if (irFunction.body == null) {
            val receiver = data.getVariableState(irFunction.getReceiver()!!) as Complex
            val instance = receiver.getOriginal()

            val functionImplementation = instance.getIrFunction(irFunction.descriptor)
            if (functionImplementation?.body == null) throw NoSuchMethodException("Method \"${irFunction.name}\" wasn't implemented")
            val arguments = functionImplementation.valueParameters.map { Variable(it.descriptor, data.getVariableState(it.descriptor)) }
            val newFrame = InterpreterFrame()
            newFrame.addVar(Variable(functionImplementation.getReceiver()!!, instance))
            newFrame.addAll(arguments)
            return functionImplementation.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
        }
        return irFunction.body!!.interpret(data)
    }

    private suspend fun calculateOverridden(owner: IrSimpleFunction, data: Frame): ExecutionResult {
        val variableDescriptor = owner.getReceiver()!!
        val superQualifier = (data.getVariableState(variableDescriptor) as? Complex)?.superClass
        if (superQualifier == null) {
            // superQualifier is null for exception state => find method in builtins
            return calculateBuiltIns(owner.getLastOverridden() as IrSimpleFunction, data)
        }
        val overridden = owner.overriddenSymbols.single()

        val newStates = InterpreterFrame(mutableListOf(Variable(overridden.owner.getReceiver()!!, superQualifier)))
        owner.valueParameters.zip(overridden.owner.valueParameters)
            .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
            .forEach { newStates.addVar(it) }

        val overriddenOwner = overridden.owner
        return when {
            overriddenOwner.body != null -> overriddenOwner.interpret(newStates)
            superQualifier.superClass == null -> calculateBuiltIns(overriddenOwner, newStates)
            else -> calculateOverridden(overriddenOwner, newStates)
        }.apply { data.pushReturnValue(newStates) }
    }

    private suspend fun calculateBuiltIns(irFunction: IrFunction, data: Frame): ExecutionResult {
        val descriptor = irFunction.descriptor
        val methodName = when (val property = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol) {
            null -> descriptor.name.asString()
            else -> property.owner.name.asString()
        }
        val args = data.getAll().map { it.state }

        val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { it.original.type }
        val argsValues = args.map { (it as? Complex)?.getOriginal() ?: (it as Primitive<*>).value }
        val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })

        val result = when (argsType.size) {
            1 -> {
                val function = unaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in unary map")
                function.invoke(argsValues.first())
            }
            2 -> {
                val function = binaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in binary map")
                when (methodName) {
                    "rangeTo" -> return calculateRangeTo(irFunction.returnType, data)
                    else -> function.invoke(argsValues[0], argsValues[1])
                }
            }
            3 -> {
                val function = ternaryFunctions[signature]
                    ?: throw NoSuchMethodException("For given function $signature there is no entry in ternary map")
                function.invoke(argsValues[0], argsValues[1], argsValues[2])
            }
            else -> throw UnsupportedOperationException("Unsupported number of arguments")
        }

        data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
        return Next
    }

    private suspend fun calculateRangeTo(type: IrType, data: Frame): ExecutionResult {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

        val primitiveValueParameters = data.getAll().map { it.state as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(primitive.type))
        }

        val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
        val newFrame = InterpreterFrame(constructorValueParameters.map { Variable(it.first, it.second) }.toMutableList())

        return constructorCall.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
    }

    private suspend fun interpretValueParameters(
        expression: IrFunctionAccessExpression, irFunction: IrFunction, data: Frame, newFrame: Frame
    ): ExecutionResult {
        // if irFunction is lambda and it has receiver, then first descriptor must be taken from extension receiver
        val receiverAsFirstArgument = when (expression.dispatchReceiver?.type?.isFunction()) {
            true -> listOfNotNull(irFunction.getExtensionReceiver())
            else -> listOf()
        }
        val valueParametersDescriptors = receiverAsFirstArgument + irFunction.descriptor.valueParameters

        val valueArguments = (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it) }
        val defaultValues = expression.symbol.owner.valueParameters.map { it.defaultValue?.expression }
        val frameWithReceiverAndData = data.copy().apply { addAll(newFrame.getAll()) } // primary use case: copy method in data class
        for ((i, valueArgument) in valueArguments.withIndex()) {
            (valueArgument ?: defaultValues[i])!!.interpret(frameWithReceiverAndData).checkForReturn(frameWithReceiverAndData, data) { return it }
            with(Variable(valueParametersDescriptors[i], frameWithReceiverAndData.popReturnValue())) {
                frameWithReceiverAndData.addVar(this)
                newFrame.addVar(this)
            }
        }
        return Next
    }

    private suspend fun interpretCall(expression: IrCall, data: Frame): ExecutionResult {
        val newFrame = InterpreterFrame()

        // dispatch receiver processing
        val rawDispatchReceiver = expression.dispatchReceiver
        rawDispatchReceiver?.interpret(data)?.check { return it }
        val dispatchReceiver = rawDispatchReceiver?.let { data.popReturnValue() }

        // extension receiver processing
        val rawExtensionReceiver = expression.extensionReceiver
        rawExtensionReceiver?.interpret(data)?.check { return it }
        val extensionReceiver = rawExtensionReceiver?.let { data.popReturnValue() }

        // find correct ir function
        val functionReceiver = dispatchReceiver?.getFunctionReceiver(expression.superQualifierSymbol?.owner)
        val irFunction = functionReceiver?.getIrFunction(expression.symbol.descriptor) ?: expression.symbol.owner

        // it is important firstly to add receiver, then arguments; this order is used in builtin method call
        irFunction.getDispatchReceiver()?.let { functionReceiver?.let { receiver -> newFrame.addVar(Variable(it, receiver)) } }
        irFunction.getExtensionReceiver()?.let { extensionReceiver?.let { receiver -> newFrame.addVar(Variable(it, receiver)) } }

        interpretValueParameters(expression, irFunction, data, newFrame).check { return it }

        // TODO fun saveReifiedParameters
        irFunction.takeIf { it.isInline }?.typeParameters?.forEachIndexed { index, typeParameter ->
            if (typeParameter.isReified) {
                val typeArgumentState = Common(expression.getTypeArgument(index)?.classOrNull!!.owner)
                newFrame.addVar(Variable(typeParameter.descriptor, typeArgumentState))
            }
        }

        if (irFunction.isInline || irFunction.isLocal) newFrame.addAll(data.getAll())

        val isWrapper = dispatchReceiver is Wrapper && rawExtensionReceiver == null
        val isInterfaceDefaultMethod = irFunction.body != null && (irFunction.parent as? IrClass)?.isInterface == true
        val executionResult = when {
            isWrapper && !isInterfaceDefaultMethod -> (dispatchReceiver as Wrapper).getMethod(irFunction).invokeMethod(irFunction, newFrame)
            irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction, newFrame)
            irFunction.isAbstract() -> calculateAbstract(irFunction, newFrame) //abstract check must be before fake overridden check
            irFunction.isFakeOverridden() -> calculateOverridden(irFunction as IrSimpleFunction, newFrame)
            irFunction.body == null || dispatchReceiver is Primitive<*> -> calculateBuiltIns(irFunction, newFrame)
            else -> irFunction.interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return executionResult
    }

    private suspend fun interpretInstanceInitializerCall(call: IrInstanceInitializerCall, data: Frame): ExecutionResult {
        val irClass = call.classSymbol.owner

        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        classProperties.forEach { property ->
            property.backingField?.initializer?.expression?.interpret(data)?.check { return it }
            val receiver = irClass.descriptor.thisAsReceiverParameter
            if (property.backingField?.initializer != null)
                property.backingField?.let { data.getVariableState(receiver).setState(Variable(it.descriptor, data.popReturnValue())) }
        }

        val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }
        anonymousInitializer.forEach { init -> init.body.interpret(data).check { return it } }

        return Next
    }

    private suspend fun interpretConstructor(constructorCall: IrFunctionAccessExpression, data: Frame): ExecutionResult {
        val owner = constructorCall.symbol.owner
        val isPrimary = (owner as IrConstructor).isPrimary
        val newFrame = InterpreterFrame()

        interpretValueParameters(constructorCall, owner, data, newFrame)

        val parent = owner.parent as IrClass
        if (parent.hasAnnotation(evaluateIntrinsicAnnotation)) {
            return when (owner.parentAsClass.getEvaluateIntrinsicValue()) {
                "kotlin.Long" -> {
                    val low = (newFrame.getVariableState(owner.valueParameters[0].descriptor) as Primitive<*>).value as Int
                    val high = (newFrame.getVariableState(owner.valueParameters[1].descriptor) as Primitive<*>).value as Int
                    data.pushReturnValue((high.toLong().shl(32) + low).toState(irBuiltIns.longType))
                    Next
                }
                "kotlin.Char" -> {
                    val value = (newFrame.getVariableState(owner.valueParameters[0].descriptor) as Primitive<*>).value as Int
                    data.pushReturnValue(value.toChar().toState(irBuiltIns.longType))
                    Next
                }
                else -> Wrapper.getConstructorMethod(owner).invokeMethod(owner, newFrame).apply { data.pushReturnValue(newFrame) }
            }
        }

        if (parent.defaultType.isArray() || parent.defaultType.isPrimitiveArray()) {
            // array constructor doesn't have body so must be treated separately
            val arrayConstructor = irBuiltIns.primitiveArrays.first().constructors.single { it.owner.valueParameters.size == 2 }
            val sizeDescriptor = arrayConstructor.owner.valueParameters.single { it.name.asString() == "size" }.descriptor
            val size = (newFrame.getVariableState(sizeDescriptor) as Primitive<*>).value as Int

            val arrayValue = MutableList<Any>(size) { 0 }
            if (owner.valueParameters.size == 2) {
                val initDescriptor = arrayConstructor.owner.valueParameters.single { it.name.asString() == "init" }.descriptor
                val initLambda = newFrame.getVariableState(initDescriptor) as Lambda
                val indexDescriptor = initLambda.irFunction.valueParameters.single().descriptor
                for (i in 0 until size) {
                    val lambdaFrame = InterpreterFrame(mutableListOf(Variable(indexDescriptor, i.toState(irBuiltIns.intType))))
                    initLambda.irFunction.body!!.interpret(lambdaFrame).check(ReturnLabel.RETURN) { return it }
                    arrayValue[i] = lambdaFrame.popReturnValue().let { (it as? Wrapper)?.value ?: (it as? Primitive<*>)?.value ?: it }
                }
            }
            data.pushReturnValue(arrayValue.toPrimitiveStateArray(parent.defaultType))
            return Next
        }

        val state = Common(parent)
        newFrame.addVar(Variable(constructorCall.getThisAsReceiver(), state)) //used to set up fields in body
        constructorCall.getBody()?.interpret(newFrame)?.checkForReturn(newFrame, data) { return it }
        val returnedState = newFrame.popReturnValue() as Complex
        data.pushReturnValue(if (isPrimary) state.apply { this.setSuperClassInstance(returnedState) } else returnedState.apply { setStatesFrom(state) })
        return Next
    }

    private suspend fun interpretConstructorCall(constructorCall: IrConstructorCall, data: Frame): ExecutionResult {
        return interpretConstructor(constructorCall, data)
    }

    private suspend fun interpretEnumConstructorCall(enumConstructorCall: IrEnumConstructorCall, data: Frame): ExecutionResult {
        return interpretConstructor(enumConstructorCall, data)
    }

    private suspend fun interpretDelegatedConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall, data: Frame): ExecutionResult {
        if (delegatingConstructorCall.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
            val anyAsStateObject = Common(irBuiltIns.anyClass.owner)
            data.pushReturnValue(anyAsStateObject)
            return Next
        }

        return interpretConstructor(delegatingConstructorCall, data)
    }

    private suspend fun interpretConst(expression: IrConst<*>, data: Frame): ExecutionResult {
        fun getSignedType(unsignedClassName: String): IrType {
            return when (unsignedClassName) {
                "UByte" -> irBuiltIns.byteType
                "UShort" -> irBuiltIns.shortType
                "UInt" -> irBuiltIns.intType
                "ULong" -> irBuiltIns.longType
                else -> throw AssertionError("")
            }
        }

        return if (UnsignedTypes.isUnsignedType(expression.type.toKotlinType())) {
            val unsignedClass = expression.type.classOrNull!!
            val constructor = unsignedClass.constructors.single().owner
            val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
            constructorCall.putValueArgument(0, expression.value.toIrConst(getSignedType(unsignedClass.owner.name.asString())))

            constructorCall.interpret(data)
        } else {
            data.pushReturnValue(expression.toPrimitive())
            Next
        }
    }

    private suspend fun interpretStatements(statements: List<IrStatement>, data: Frame): ExecutionResult {
        var executionResult: ExecutionResult = Next
        val iterator = statements.iterator()
        while (executionResult.returnLabel == ReturnLabel.NEXT && iterator.hasNext()) {
            executionResult = iterator.next().interpret(data)
        }
        return executionResult
    }

    private suspend fun interpretBlock(block: IrBlock, data: Frame): ExecutionResult {
        val newFrame = data.copy()
        return interpretStatements(block.statements, newFrame).apply { data.pushReturnValue(newFrame) }
    }

    private suspend fun interpretBody(body: IrBody, data: Frame): ExecutionResult {
        val newFrame = data.copy()
        return interpretStatements(body.statements, newFrame).apply { data.pushReturnValue(newFrame) }
    }

    private suspend fun interpretReturn(expression: IrReturn, data: Frame): ExecutionResult {
        val executionResult = expression.value.interpret(data)
        return when (executionResult.returnLabel) {
            ReturnLabel.NEXT -> Return.addInfo(expression.returnTargetSymbol.descriptor.toString())
            else -> executionResult
        }
    }

    private suspend fun interpretWhile(expression: IrWhileLoop, data: Frame): ExecutionResult {
        var executionResult: ExecutionResult = Next
        while (executionResult.returnLabel == ReturnLabel.NEXT) {
            executionResult = expression.condition.interpret(data)
            if (executionResult.returnLabel == ReturnLabel.NEXT && (data.popReturnValue() as? Primitive<*>)?.value as? Boolean == true) {
                executionResult = expression.body?.interpret(data) ?: Next
            } else {
                break
            }
        }
        return executionResult
    }

    private suspend fun interpretWhen(expression: IrWhen, data: Frame): ExecutionResult {
        var executionResult: ExecutionResult = Next
        val iterator = expression.branches.asSequence().iterator()
        while (executionResult.returnLabel == ReturnLabel.NEXT && iterator.hasNext()) {
            executionResult = iterator.next().interpret(data)
        }
        return executionResult
    }

    private suspend fun interpretBranch(expression: IrBranch, data: Frame): ExecutionResult {
        var executionResult = expression.condition.interpret(data)
        if (executionResult.returnLabel == ReturnLabel.NEXT && (data.popReturnValue() as? Primitive<*>)?.value as? Boolean == true) {
            executionResult = expression.result.interpret(data)
            if (executionResult.returnLabel == ReturnLabel.NEXT) return BreakWhen
        }
        return executionResult
    }

    private suspend fun interpretBreak(breakStatement: IrBreak, data: Frame): ExecutionResult {
        return BreakLoop.addInfo(breakStatement.label ?: "")
    }

    private suspend fun interpretContinue(continueStatement: IrContinue, data: Frame): ExecutionResult {
        return Continue.addInfo(continueStatement.label ?: "")
    }

    private suspend fun interpretSetField(expression: IrSetField, data: Frame): ExecutionResult {
        val executionResult = expression.value.interpret(data)
        if (executionResult.returnLabel != ReturnLabel.NEXT) return executionResult

        // receiver is null only for top level var, but it cannot be used in constexpr; corresponding check is on frontend
        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        data.getVariableState(receiver).setState(Variable(expression.symbol.owner.descriptor, data.popReturnValue()))
        return Next
    }

    private suspend fun interpretGetField(expression: IrGetField, data: Frame): ExecutionResult {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
        val result = receiver?.let { data.getVariableState(receiver).getState(expression.symbol.descriptor)?.copy() }
        if (result == null) {
            return expression.symbol.owner.initializer?.expression?.interpret(data) ?: Next
        }
        data.pushReturnValue(result)
        return Next
    }

    private suspend fun interpretGetValue(expression: IrGetValue, data: Frame): ExecutionResult {
        data.pushReturnValue(data.getVariableState(expression.symbol.descriptor).copy())
        return Next
    }

    private suspend fun interpretVariable(expression: IrVariable, data: Frame): ExecutionResult {
        val executionResult = expression.initializer?.interpret(data)// TODO replace with check
        if (executionResult?.returnLabel != ReturnLabel.NEXT) return executionResult ?: Next
        data.addVar(Variable(expression.descriptor, data.popReturnValue()))
        return Next
    }

    private suspend fun interpretSetVariable(expression: IrSetVariable, data: Frame): ExecutionResult {
        val executionResult = expression.value.interpret(data)
        if (executionResult.returnLabel != ReturnLabel.NEXT) return executionResult

        if (data.contains(expression.symbol.descriptor)) {
            val variable = data.getVariableState(expression.symbol.descriptor)
            variable.setState(Variable(expression.symbol.descriptor, data.popReturnValue()))
        } else {
            data.addVar(Variable(expression.symbol.descriptor, data.popReturnValue()))
        }
        return Next
    }

    private suspend fun interpretGetObjectValue(expression: IrGetObjectValue, data: Frame): ExecutionResult {
        val owner = expression.symbol.owner
        if (owner.hasAnnotation(evaluateIntrinsicAnnotation)) {
            data.pushReturnValue(Wrapper.getCompanionObject(owner))
            return Next
        }
        val objectState = Common(owner)
        data.pushReturnValue(objectState)
        return Next
    }

    private suspend fun interpretGetEnumValue(expression: IrGetEnumValue, data: Frame): ExecutionResult {
        val enumEntry = expression.symbol.owner
        val enumSignature = Pair(enumEntry.parentAsClass, enumEntry.name.asString())
        mapOfEnums[enumSignature]?.let { return Next.apply { data.pushReturnValue(it) } }

        val enumClass = enumEntry.symbol.owner.parentAsClass
        if (enumClass.hasAnnotation(evaluateIntrinsicAnnotation)) {
            val valueOfFun = enumClass.declarations.single { it.nameForIrSerialization.asString() == "valueOf" } as IrFunction
            val enumName = Variable(valueOfFun.valueParameters.first().descriptor, enumEntry.name.asString().toState(irBuiltIns.stringType))
            val newFrame = InterpreterFrame(mutableListOf(enumName))
            return Wrapper.getEnumEntry(enumClass)!!.invokeMethod(valueOfFun, newFrame).apply {
                if (this.returnLabel == ReturnLabel.NEXT) mapOfEnums[enumSignature] = newFrame.peekReturnValue() as Wrapper
                data.pushReturnValue(newFrame)
            }
        }

        return interpretEnumEntry(enumEntry, data).apply {
            if (this.returnLabel == ReturnLabel.NEXT) mapOfEnums[enumSignature] = data.peekReturnValue() as Common
        }
    }

    private suspend fun interpretEnumEntry(enumEntry: IrEnumEntry, data: Frame): ExecutionResult {
        val enumClass = enumEntry.symbol.owner.parentAsClass
        val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()

        val enumSuperCall = (enumClass.primaryConstructor?.body?.statements?.firstOrNull() as? IrEnumConstructorCall)
        if (enumEntries.isNotEmpty() && enumSuperCall != null) {
            val valueArguments = listOf(
                enumEntry.name.asString().toIrConst(irBuiltIns.stringType), enumEntries.indexOf(enumEntry).toIrConst(irBuiltIns.intType)
            )
            enumSuperCall.mapValueParameters { valueArguments[it.index] }
        }

        val executionResult = enumEntry.initializerExpression?.interpret(data)?.check { return it }
        enumSuperCall?.mapValueParameters { null }
        data.pushReturnValue((data.popReturnValue() as Complex))
        return executionResult ?: throw AssertionError("Initializer at enum entry ${enumEntry.fqNameWhenAvailable} is null")
    }

    private suspend fun interpretTypeOperatorCall(expression: IrTypeOperatorCall, data: Frame): ExecutionResult {
        val executionResult = expression.argument.interpret(data).check { return it }

        return when (expression.operator) {
            // coercion to unit means that return value isn't used
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> executionResult
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                if (!data.peekReturnValue().irClass.defaultType.isSubtypeOf(expression.type, irBuiltIns)) {
                    val convertibleClassName = data.popReturnValue().irClass.fqNameForIrSerialization
                    val castClassName = expression.type.classOrNull?.owner?.fqNameForIrSerialization
                    val message = "$convertibleClassName cannot be cast to $castClassName"
                    data.pushReturnValue(ExceptionState(ClassCastException(message), classCastException, stackTrace))
                    Exception
                } else {
                    executionResult
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                if (!data.peekReturnValue().irClass.defaultType.isSubtypeOf(expression.type, irBuiltIns)) {
                    data.popReturnValue()
                    data.pushReturnValue(null.toState(irBuiltIns.nothingType))
                }
                executionResult
            }
            IrTypeOperator.INSTANCEOF -> {
                val isInstance = data.popReturnValue().irClass.defaultType.isSubtypeOf(expression.typeOperand, irBuiltIns)
                data.pushReturnValue(isInstance.toState(irBuiltIns.nothingType))
                executionResult
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val isInstance = data.popReturnValue().irClass.defaultType.isSubtypeOf(expression.typeOperand, irBuiltIns)
                data.pushReturnValue((!isInstance).toState(irBuiltIns.nothingType))
                executionResult
            }
            else -> TODO("${expression.operator} not implemented")
        }
    }

    private suspend fun interpretVararg(expression: IrVararg, data: Frame): ExecutionResult {
        val args = expression.elements.flatMap {
            it.interpret(data).check { return it }
            return@flatMap when (val result = data.popReturnValue()) {
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
        data.pushReturnValue(args.toPrimitiveStateArray(expression.type))
        return Next
    }

    private suspend fun interpretSpreadElement(spreadElement: IrSpreadElement, data: Frame): ExecutionResult {
        return spreadElement.expression.interpret(data).check { return it }
    }

    private suspend fun interpretTry(expression: IrTry, data: Frame): ExecutionResult {
        var executionResult = expression.tryResult.interpret(data)
        if (executionResult.returnLabel == ReturnLabel.EXCEPTION) {
            val exception = data.peekReturnValue() as ExceptionState
            for (catchBlock in expression.catches) {
                if (exception.isSubtypeOf(catchBlock.catchParameter.type.classOrNull!!.owner)) {
                    executionResult = catchBlock.interpret(data)
                    break
                }
            }
        }
        // TODO check flow correctness; should I return finally result code if in catch there was an exception?
        return expression.finallyExpression?.interpret(data) ?: executionResult
    }

    private suspend fun interpretCatch(expression: IrCatch, data: Frame): ExecutionResult {
        val newFrame = InterpreterFrame(data.getAll().toMutableList())
        newFrame.addVar(Variable(expression.parameter, data.popReturnValue()))
        return expression.result.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
    }

    private suspend fun interpretThrow(expression: IrThrow, data: Frame): ExecutionResult {
        expression.value.interpret(data).check { return it }
        when (val exception = data.popReturnValue()) {
            is Common -> data.pushReturnValue(ExceptionState(exception, stackTrace))
            is Wrapper -> data.pushReturnValue(ExceptionState(exception, stackTrace))
            is ExceptionState -> data.pushReturnValue(exception)
            else -> throw AssertionError("${exception::class} cannot be used as exception state")
        }
        return Exception
    }

    private suspend fun interpretStringConcatenation(expression: IrStringConcatenation, data: Frame): ExecutionResult {
        val result = StringBuilder()
        expression.arguments.forEach {
            it.interpret(data).check { executionResult -> return executionResult }
            result.append(
                when (val returnValue = data.popReturnValue()) {
                    is Primitive<*> -> returnValue.value.toString()
                    is Wrapper -> returnValue.value.toString()
                    is Common -> {
                        val toStringFun = returnValue.getToStringFunction()
                        val newFrame = InterpreterFrame(mutableListOf(Variable(toStringFun.getReceiver()!!, returnValue)))
                        val executionResult = toStringFun.body?.let { toStringFun.interpret(newFrame) } ?: calculateOverridden(toStringFun, newFrame)
                        if (executionResult.returnLabel != ReturnLabel.NEXT) return executionResult
                        (newFrame.popReturnValue() as Primitive<*>).value.toString()
                    }
                    else -> throw AssertionError("$returnValue cannot be used in StringConcatenation expression")
                }
            )
        }

        data.pushReturnValue(result.toString().toState(expression.type))
        return Next
    }

    private suspend fun interpretFunctionExpression(expression: IrFunctionExpression, data: Frame): ExecutionResult {
        data.pushReturnValue(Lambda(expression.function, expression.type.classOrNull!!.owner))
        return Next
    }

    private suspend fun interpretFunctionReference(reference: IrFunctionReference, data: Frame): ExecutionResult {
        data.pushReturnValue(Lambda(reference.symbol.owner, reference.type.classOrNull!!.owner))
        return Next
    }

    private suspend fun interpretComposite(expression: IrComposite, data: Frame): ExecutionResult {
        return when (expression.origin) {
            IrStatementOrigin.DESTRUCTURING_DECLARATION -> interpretStatements(expression.statements, data)
            else -> TODO("${expression.origin} not implemented")
        }
    }
}