/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import org.jetbrains.kotlin.backend.common.interpreter.builtins.*
import org.jetbrains.kotlin.backend.common.interpreter.stack.*
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrErrorExpressionImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isSubtypeOf
import org.jetbrains.kotlin.ir.util.*
import java.lang.invoke.MethodHandle

enum class Code(var info: String = "") {
    NEXT, RETURN, BREAK_LOOP, BREAK_WHEN, CONTINUE, EXCEPTION
}

class IrInterpreter(irModule: IrModuleFragment) {
    private val irBuiltIns = irModule.irBuiltins
    private val irExceptions = irModule.files.flatMap { it.declarations }.filterIsInstance<IrClass>()
        .filter { it.isSubclassOf(irBuiltIns.throwableClass.owner) }
    private val classCastException = irExceptions.first { it.name.asString() == ClassCastException::class.java.simpleName }

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

    fun interpret(expression: IrExpression): IrExpression {
        val data = InterpreterFrame()
        return when (val code = expression.interpret(data)) {
            Code.NEXT -> data.popReturnValue().toIrExpression(expression)
            Code.EXCEPTION -> {
                val message = when (val exception = data.popReturnValue()) {
                    is Wrapper -> (exception.value as Throwable).message.toString()
                    is Complex -> (exception.fields.first { it.descriptor.name.asString() == "message" }.state as Primitive<*>).value.toString()
                    else -> TODO("not supported")
                }
                IrErrorExpressionImpl(expression.startOffset, expression.endOffset, expression.type, message)
            }
            else -> TODO("$code not supported as result of interpretation")
        }
    }

    private fun IrElement.interpret(data: Frame): Code {
        try {
            val code = when (this) {
                is IrFunctionImpl -> interpretFunction(this, data)
                is IrCall -> interpretCall(this, data)
                is IrConstructorCall -> interpretConstructorCall(this, data)
                is IrDelegatingConstructorCall -> interpretDelegatedConstructorCall(this, data)
                is IrBody -> interpretBody(this, data)
                is IrBlock -> interpretBlock(this, data)
                is IrReturn -> interpretReturn(this, data)
                is IrSetField -> interpretSetField(this, data)
                is IrGetField -> interpretGetField(this, data)
                is IrGetValue -> interpretGetValue(this, data)
                is IrGetObjectValue -> interpretGetObjectValue(this, data)
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
                is IrTry -> interpretTry(this, data)
                is IrCatch -> interpretCatch(this, data)
                is IrThrow -> interpretThrow(this, data)
                is IrStringConcatenation -> interpretStringConcatenation(this, data)

                else -> TODO("${this.javaClass} not supported")
            }

            return when (code) {
                Code.RETURN -> when (this) { // TODO check label
                    is IrCall, is IrReturnableBlock, is IrFunctionImpl -> Code.NEXT
                    else -> Code.RETURN
                }
                Code.BREAK_WHEN -> when (this) {
                    is IrWhen -> Code.NEXT
                    else -> code
                }
                Code.BREAK_LOOP -> when (this) {
                    is IrWhileLoop -> if ((this.label ?: "") == code.info) Code.NEXT else code
                    else -> code
                }
                Code.CONTINUE -> when (this) {
                    is IrWhileLoop -> if ((this.label ?: "") == code.info) this.interpret(data) else code
                    else -> code
                }
                Code.EXCEPTION -> Code.EXCEPTION
                Code.NEXT -> Code.NEXT
            }
        } catch (e: Exception) { // TODO catch Throwable
            // catch exception from JVM such as: ArithmeticException, StackOverflowError and others
            val exceptionName = e::class.java.simpleName
            val irExceptionClass = irExceptions.firstOrNull { it.name.asString() == exceptionName }

            if (irExceptionClass == null && exceptionName == "KotlinNullPointerException") {
                // this block is used to replace jvm null pointer exception with common one
                // TODO figure something better
                data.pushReturnValue(Wrapper(e, irExceptions.first { it.name.asString() == "NullPointerException" }))
            } else {
                irExceptionClass?.let { data.pushReturnValue(Wrapper(e, it)) } ?: throw AssertionError("Cannot handle exception $e")
            }

            return Code.EXCEPTION
        }
    }

    // this method is used to get stack trace after exception
    // OR
    // TODO maybe use suspend
    private fun interpretFunction(irFunction: IrFunctionImpl, data: Frame): Code {
        return irFunction.body?.interpret(data) ?: throw AssertionError("Ir function must be with body")
    }

    private fun MethodHandle.invokeMethod(irFunction: IrFunction, data: Frame): Code {
        // TODO try catch
        val result = this.invokeWithArguments(irFunction.getArgsForMethodInvocation(data))
        data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))

        return Code.NEXT
    }

    private fun calculateAbstract(irFunction: IrFunction, data: Frame): Code {
        if (irFunction.body == null) {
            val receiver = data.getVariableState(irFunction.symbol.getReceiver()!!) as Complex
            val instance = receiver.instance!!

            val functionImplementation = instance.getIrFunction(irFunction.descriptor)
            if (functionImplementation?.body == null) throw NoSuchMethodException("Method \"${irFunction.name}\" wasn't implemented")
            val arguments = functionImplementation.valueParameters.map { Variable(it.descriptor, data.getVariableState(it.descriptor)) }
            val newFrame = InterpreterFrame()
            newFrame.addVar(Variable(functionImplementation.symbol.getReceiver()!!, instance))
            newFrame.addAll(arguments)
            return functionImplementation.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
        }
        return irFunction.body!!.interpret(data)
    }

    private fun calculateOverridden(owner: IrFunctionImpl, data: Frame): Code {
        val variableDescriptor = owner.symbol.getReceiver()!!
        val superQualifier = (data.getVariableState(variableDescriptor) as Complex).superType!!
        val overridden = owner.overriddenSymbols.first { it.getReceiver()?.equalTo(superQualifier.getReceiver()) == true }

        val newStates = InterpreterFrame(mutableListOf(Variable(overridden.getReceiver()!!, superQualifier)))
        owner.valueParameters.zip(overridden.owner.valueParameters)
            .map { Variable(it.second.descriptor, data.getVariableState(it.first.descriptor)) }
            .forEach { newStates.addVar(it) }

        val overriddenOwner = overridden.owner as IrFunctionImpl
        return when {
            overriddenOwner.body != null -> overriddenOwner.interpret(newStates)
            superQualifier.superType == null -> calculateBuiltIns(overriddenOwner, newStates)
            else -> calculateOverridden(overriddenOwner, newStates)
        }.apply { data.pushReturnValue(newStates) }
    }

    private fun calculateBuiltIns(irFunction: IrFunction, data: Frame): Code {
        val descriptor = irFunction.descriptor
        val methodName = descriptor.name.asString()
        val args = data.getAll().map { it.state }

        val result: Any?
        if (irFunction.parent.fqNameForIrSerialization.toString() == "kotlin.Any" || args.any { it !is Primitive<*> }) {
            // if ir function is declaration in Any class OR it is ir builtin operator for non primitive types
            val receiver = (args[0] as Complex).instance
            result = when (methodName) {
                "equals", "EQEQ", "EQEQEQ" -> (args[1] as Complex).instance === receiver
                "hashCode" -> System.identityHashCode(receiver)
                "toString" -> receiver?.irClass?.name?.asString() + "@" + System.identityHashCode(receiver).toString(16).padStart(8)
                else -> throw IllegalStateException("Method $methodName can not be interpreted")
            }
        } else {
            val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
            val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { it.original.type }
            val argsValues = args
                .map { it as? Primitive<*> ?: throw IllegalArgumentException("Builtin functions accept only const args") }
                .map { it.value }
            val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })

            result = when (argsType.size) {
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
        }
        data.pushReturnValue(result.toState(result.getType(irFunction.returnType)))
        return Code.NEXT
    }

    private fun calculateRangeTo(type: IrType, data: Frame): Code {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

        val primitiveValueParameters = data.getAll().map { it.state as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(primitive.type))
        }

        val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
        val newFrame = InterpreterFrame(constructorValueParameters.map { Variable(it.first, it.second) }.toMutableList())

        val code = constructorCall.interpret(newFrame)
        data.pushReturnValue(newFrame)
        return code
    }

    private fun interpretValueParameters(parametersContainer: IrMemberAccessExpression, data: Frame): Code {
        for (i in (parametersContainer.valueArgumentsCount - 1) downTo 0) {
            val code = parametersContainer.getValueArgument(i)?.interpret(data) ?: Code.NEXT
            if (code != Code.NEXT) return code
        }
        return Code.NEXT
    }

    private fun interpretCall(expression: IrCall, data: Frame): Code {
        val newFrame = InterpreterFrame()

        // dispatch receiver processing
        val rawDispatchReceiver = expression.dispatchReceiver
        rawDispatchReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }
        val dispatchReceiver = rawDispatchReceiver?.let { data.popReturnValue() }
        val irFunctionReceiver = if (expression.superQualifierSymbol == null) dispatchReceiver else (dispatchReceiver as Complex).superType
        // it is important firstly to add receiver, then arguments; this order is used in builtin method call
        val irFunction = irFunctionReceiver?.getIrFunction(expression.symbol.descriptor) ?: expression.symbol.owner
        irFunctionReceiver?.let { receiverState ->
            // dispatch receiver is null if irFunction is lambda and so there is no need to add it as variable in frame
            irFunction.symbol.getDispatchReceiver()?.let { newFrame.addVar(Variable(it, receiverState)) }
        }

        // extension receiver processing
        val rawExtensionReceiver = expression.extensionReceiver
        rawExtensionReceiver?.interpret(data)?.also { if (it != Code.NEXT) return it }
        val extensionReceiver = rawExtensionReceiver?.let { data.popReturnValue() }
        extensionReceiver?.let { newFrame.addVar(Variable(irFunction.symbol.getExtensionReceiver()!!, it)) }

        // if irFunction is lambda and it has receiver, then first descriptor must be taken from extension receiver
        val receiverAsFirstArgument = if (irFunction.isLocalLambda()) listOfNotNull(irFunction.symbol.getExtensionReceiver()) else listOf()
        val valueParametersDescriptors = receiverAsFirstArgument + irFunction.descriptor.valueParameters
        interpretValueParameters(expression, data).also { if (it != Code.NEXT) return it }
        newFrame.addAll(valueParametersDescriptors.map { Variable(it, data.popReturnValue()) })

        val isWrapper = (dispatchReceiver is Wrapper && rawExtensionReceiver == null) ||
                (extensionReceiver is Wrapper && rawDispatchReceiver == null)
        val code = when {
            isWrapper -> ((dispatchReceiver ?: extensionReceiver) as Wrapper).getMethod(irFunction).invokeMethod(irFunction, newFrame)
            irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction, newFrame)
            irFunction.isAbstract() -> calculateAbstract(irFunction, newFrame) //abstract check must be before fake overridden check
            irFunction.isFakeOverridden() -> calculateOverridden(irFunction as IrFunctionImpl, newFrame)
            irFunction.body == null -> calculateBuiltIns(irFunction, newFrame)
            else -> irFunction.interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return code
    }

    private fun interpretConstructor(constructorCall: IrFunctionAccessExpression, data: Frame): Code {
        interpretValueParameters(constructorCall, data).also { if (it != Code.NEXT) return it }
        val valueParameters = constructorCall.symbol.descriptor.valueParameters.map { Variable(it, data.popReturnValue()) }.toMutableList()
        val newFrame = InterpreterFrame(valueParameters)

        val owner = constructorCall.symbol.owner
        val parent = owner.parent as IrClass
        if (parent.hasAnnotation(evaluateIntrinsicAnnotation)) {
            return Wrapper.getConstructorMethod(owner).invokeMethod(owner, newFrame).apply { data.pushReturnValue(newFrame) }
        }

        val state = Complex(parent, mutableListOf())
        newFrame.addVar(Variable(constructorCall.getThisAsReceiver(), state)) //used to set up fields in body
        val code = constructorCall.getBody()?.interpret(newFrame) ?: Code.NEXT
        state.superType = newFrame.popReturnValue() as Complex
        data.pushReturnValue(state)
        return code
    }

    private fun interpretConstructorCall(constructorCall: IrConstructorCall, data: Frame): Code {
        return interpretConstructor(constructorCall, data).apply {
            val instance = data.peekReturnValue() as Complex
            instance.setInstanceRecursive(instance)
        }
    }

    private fun interpretDelegatedConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall, data: Frame): Code {
        if (delegatingConstructorCall.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
            val anyComplex = Complex(irBuiltIns.anyClass.owner, mutableListOf())
            data.pushReturnValue(anyComplex)
            return Code.NEXT
        }

        return interpretConstructor(delegatingConstructorCall, data)
    }

    private fun interpretConst(expression: IrConst<*>, data: Frame): Code {
        data.pushReturnValue(expression.toPrimitive())
        return Code.NEXT
    }

    private fun interpretStatements(statements: List<IrStatement>, data: Frame): Code {
        val newFrame = data.copy()

        var code = Code.NEXT
        val iterator = statements.asSequence().iterator()
        while (code == Code.NEXT && iterator.hasNext()) {
            code = iterator.next().interpret(newFrame)
        }
        data.pushReturnValue(newFrame)
        return code
    }

    private fun interpretBlock(block: IrBlock, data: Frame): Code {
        if (block.isLambdaFunction()) {
            val irLambdaFunction = block.statements.filterIsInstance<IrFunctionReference>().first()
            data.pushReturnValue(Lambda(irLambdaFunction.symbol.owner, irLambdaFunction.type.classOrNull!!.owner))
            return Code.NEXT
        }
        return interpretStatements(block.statements, data)
    }

    private fun interpretBody(body: IrBody, data: Frame): Code {
        return interpretStatements(body.statements, data)
    }

    private fun interpretReturn(expression: IrReturn, data: Frame): Code {
        val code = expression.value.interpret(data)
        return if (code == Code.NEXT) Code.RETURN else code
    }

    private fun interpretWhile(expression: IrWhileLoop, data: Frame): Code {
        var code = Code.NEXT
        while (code == Code.NEXT) {
            code = expression.condition.interpret(data)
            if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.value as? Boolean == true) {
                code = expression.body?.interpret(data) ?: Code.NEXT
            } else {
                break
            }
        }
        return code
    }

    private fun interpretWhen(expression: IrWhen, data: Frame): Code {
        var code = Code.NEXT
        val iterator = expression.branches.asSequence().iterator()
        while (code == Code.NEXT && iterator.hasNext()) {
            code = iterator.next().interpret(data)
        }
        return code
    }

    private fun interpretBranch(expression: IrBranch, data: Frame): Code {
        var code = expression.condition.interpret(data)
        if (code == Code.NEXT && (data.popReturnValue() as? Primitive<*>)?.value as? Boolean == true) {
            code = expression.result.interpret(data)
            if (code == Code.NEXT) return Code.BREAK_WHEN
        }
        return code
    }

    private fun interpretBreak(breakStatement: IrBreak, data: Frame): Code {
        return Code.BREAK_LOOP.apply { info = breakStatement.label ?: "" }
    }

    private fun interpretContinue(continueStatement: IrContinue, data: Frame): Code {
        return Code.CONTINUE.apply { info = continueStatement.label ?: "" }
    }

    private fun interpretSetField(expression: IrSetField, data: Frame): Code {
        val code = expression.value.interpret(data)
        if (code != Code.NEXT) return code

        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        data.getVariableState(receiver).setState(Variable(expression.symbol.owner.descriptor, data.popReturnValue()))
        return Code.NEXT
    }

    private fun interpretGetField(expression: IrGetField, data: Frame): Code {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor // receiver is null, for example, for top level fields
        val result = receiver?.let { data.getVariableState(receiver).getState(expression.symbol.descriptor)?.copy() }
        if (result == null) {
            return expression.symbol.owner.initializer?.expression?.interpret(data) ?: Code.NEXT
        }
        data.pushReturnValue(result)
        return Code.NEXT
    }

    private fun interpretGetValue(expression: IrGetValue, data: Frame): Code {
        data.pushReturnValue(data.getVariableState(expression.symbol.descriptor).copy())
        return Code.NEXT
    }

    private fun interpretVariable(expression: IrVariable, data: Frame): Code {
        val code = expression.initializer?.interpret(data)
        if (code != Code.NEXT) return code ?: Code.NEXT
        data.addVar(Variable(expression.descriptor, data.popReturnValue()))
        return Code.NEXT
    }

    private fun interpretSetVariable(expression: IrSetVariable, data: Frame): Code {
        val code = expression.value.interpret(data)
        if (code != Code.NEXT) return code

        if (data.contains(expression.symbol.descriptor)) {
            val variable = data.getVariableState(expression.symbol.descriptor)
            variable.setState(Variable(expression.symbol.descriptor, data.popReturnValue()))
        } else {
            data.addVar(Variable(expression.symbol.descriptor, data.popReturnValue()))
        }
        return Code.NEXT
    }

    private fun interpretGetObjectValue(expression: IrGetObjectValue, data: Frame): Code {
        val objectState = Complex(expression.symbol.owner, mutableListOf())
        val newFrame = data.copy().apply { addVar(Variable(expression.symbol.descriptor.thisAsReceiverParameter, objectState)) }

        val constructor = expression.symbol.owner.declarations.single { it is IrConstructor } as IrConstructor
        val constructorBody = constructor.body?.statements?.get(1) as? IrBlock
        val irAnnotatedSetFields = constructorBody?.statements?.filterIsInstance<IrSetField>()
            ?.filter { it.symbol.owner.correspondingPropertySymbol!!.owner.hasAnnotation(compileTimeAnnotation) }
        irAnnotatedSetFields?.forEach { irSetField -> irSetField.interpret(newFrame).also { if (it != Code.NEXT) return it } }

        data.pushReturnValue(objectState)
        return Code.NEXT
    }

    private fun interpretTypeOperatorCall(expression: IrTypeOperatorCall, data: Frame): Code {
        return when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT, IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> {
                expression.argument.interpret(data)
            }
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                val code = expression.argument.interpret(data)
                if (!data.peekReturnValue().irClass.defaultType.isSubtypeOf(expression.type, irBuiltIns)) {
                    val convertibleClassName = data.popReturnValue().irClass.fqNameForIrSerialization
                    val castClassName = expression.type.classOrNull?.owner?.fqNameForIrSerialization
                    val message = "$convertibleClassName cannot be cast to $castClassName"
                    data.pushReturnValue(Wrapper(ClassCastException(message), classCastException))
                    Code.EXCEPTION
                } else {
                    code
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                val code = expression.argument.interpret(data)
                if (!data.peekReturnValue().irClass.defaultType.isSubtypeOf(expression.type, irBuiltIns)) {
                    data.popReturnValue()
                    data.pushReturnValue(null.toState(irBuiltIns.nothingType))
                }
                code
            }
            else -> TODO("${expression.operator} not implemented")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun interpretVararg(expression: IrVararg, data: Frame): Code {
        val args = expression.elements.map {
            it.interpret(data).apply { if (this != Code.NEXT) return this }
            data.popReturnValue()
        }
        val type = expression.type
        val array = when (type.getFqName()) {
            "kotlin.ByteArray" -> Primitive(args.map { (it as Primitive<Byte>).value }.toByteArray(), type)
            "kotlin.CharArray" -> Primitive(args.map { (it as Primitive<Char>).value }.toCharArray(), type)
            "kotlin.ShortArray" -> Primitive(args.map { (it as Primitive<Short>).value }.toShortArray(), type)
            "kotlin.IntArray" -> Primitive(args.map { (it as Primitive<Int>).value }.toIntArray(), type)
            "kotlin.LongArray" -> Primitive(args.map { (it as Primitive<Long>).value }.toLongArray(), type)
            "kotlin.FloatArray" -> Primitive(args.map { (it as Primitive<Float>).value }.toFloatArray(), type)
            "kotlin.DoubleArray" -> Primitive(args.map { (it as Primitive<Double>).value }.toDoubleArray(), type)
            "kotlin.BooleanArray" -> Primitive(args.map { (it as Primitive<Boolean>).value }.toBooleanArray(), type)
            else -> Primitive<Array<*>>(args.map { (it as? Wrapper)?.value ?: (it as? Primitive<*>)?.value ?: it }.toTypedArray(), type)
        }
        data.pushReturnValue(array)

        return Code.NEXT
    }

    private fun interpretTry(expression: IrTry, data: Frame): Code {
        var code = expression.tryResult.interpret(data)
        if (code == Code.EXCEPTION) {
            val exception = data.peekReturnValue()
            for (catchBlock in expression.catches) {
                if (exception.irClass.defaultType.isSubtypeOf(catchBlock.catchParameter.type, irBuiltIns)) {
                    code = catchBlock.interpret(data)
                    break
                }
            }
        }
        return expression.finallyExpression?.interpret(data) ?: code
    }

    private fun interpretCatch(expression: IrCatch, data: Frame): Code {
        val newFrame = InterpreterFrame(data.getAll().toMutableList())
        newFrame.addVar(Variable(expression.parameter, data.popReturnValue()))
        return expression.result.interpret(newFrame).apply { data.pushReturnValue(newFrame) }
    }

    private fun interpretThrow(expression: IrThrow, data: Frame): Code {
        expression.value.interpret(data)
        return Code.EXCEPTION
    }

    private fun interpretStringConcatenation(expression: IrStringConcatenation, data: Frame): Code {
        val result = StringBuilder()
        expression.arguments.forEach {
            it.interpret(data).also { code -> if (code != Code.NEXT) return code }
            result.append(
                when (val returnValue = data.popReturnValue()) {
                    is Primitive<*> -> returnValue.value.toString()
                    is Wrapper -> returnValue.value.toString()
                    is Complex -> {
                        val toStringFun = returnValue.getToStringFunction()
                        val newFrame = InterpreterFrame(mutableListOf(Variable(toStringFun.symbol.getReceiver()!!, returnValue)))
                        val code = toStringFun.body?.let { toStringFun.interpret(newFrame) } ?: calculateOverridden(toStringFun, newFrame)
                        if (code != Code.NEXT) return code
                        (newFrame.popReturnValue() as Primitive<*>).value.toString()
                    }
                    else -> throw AssertionError("$returnValue cannot be used in StringConcatenation expression")
                }
            )
        }

        data.pushReturnValue(result.toString().toState(expression.type))
        return Code.NEXT
    }
}