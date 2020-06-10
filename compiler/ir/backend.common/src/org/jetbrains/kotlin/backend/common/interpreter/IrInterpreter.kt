/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.interpreter

import kotlinx.coroutines.*
import org.jetbrains.kotlin.backend.common.interpreter.builtins.*
import org.jetbrains.kotlin.backend.common.interpreter.exceptions.InterpreterException
import org.jetbrains.kotlin.backend.common.interpreter.exceptions.InterpreterMethodNotFoundException
import org.jetbrains.kotlin.backend.common.interpreter.exceptions.InterpreterTimeOutException
import org.jetbrains.kotlin.backend.common.interpreter.intrinsics.IntrinsicEvaluator
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

    private val stack = StackImpl()
    private var commandCount = 0

    companion object {
        private val mapOfEnums = mutableMapOf<String, Complex>()
        private val mapOfObjects = mutableMapOf<String, Complex>()
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
            null -> irBuiltIns.nothingType
            else -> when (defaultType.classifierOrNull?.owner) {
                is IrTypeParameter -> stack.getVariableState(defaultType.classifierOrFail.descriptor).irClass.defaultType
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
        return try {
            runBlocking {
                return@runBlocking when (val returnLabel = withContext(this.coroutineContext) { expression.interpret().returnLabel }) {
                    ReturnLabel.NEXT -> stack.popReturnValue().toIrExpression(expression)
                    ReturnLabel.EXCEPTION -> {
                        val message = (stack.popReturnValue() as ExceptionState).getFullDescription()
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

    private suspend fun IrElement.interpret(): ExecutionResult {
        try {
            incrementAndCheckCommands()
            val executionResult = when (this) {
                is IrFunctionImpl -> interpretFunction(this)
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
                is IrSetVariable -> interpretSetVariable(this)
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
    private suspend fun interpretFunction(irFunction: IrFunctionImpl): ExecutionResult {
        yield()

        if (irFunction.fileOrNull != null) stack.setCurrentFrameName(irFunction)

        if (stack.getStackTrace().size == MAX_STACK_SIZE) throw StackOverflowError("")

        if (irFunction.body is IrSyntheticBody) return handleIntrinsicMethods(irFunction)
        return irFunction.body?.interpret() ?: throw InterpreterException("Ir function must be with body")
    }

    private suspend fun MethodHandle?.invokeMethod(irFunction: IrFunction): ExecutionResult {
        this ?: return handleIntrinsicMethods(irFunction)
        val result = this.invokeWithArguments(irFunction.getArgsForMethodInvocation(stack.getAll()))
        stack.pushReturnValue(result.toState(result.getType(irFunction.returnType)))

        return Next
    }

    private suspend fun handleIntrinsicMethods(irFunction: IrFunction): ExecutionResult {
        return IntrinsicEvaluator().evaluate(irFunction, stack) { this.interpret() }
    }

    private suspend fun calculateAbstract(irFunction: IrFunction): ExecutionResult {
        if (irFunction.body == null) {
            val receiver = stack.getVariableState(irFunction.getReceiver()!!) as Complex
            val instance = receiver.getOriginal()

            val functionImplementation = instance.getIrFunction(irFunction.descriptor)
            if (functionImplementation?.body == null) throw InterpreterMethodNotFoundException("Method \"${irFunction.name}\" wasn't implemented")

            val valueArguments = mutableListOf<Variable>()
            valueArguments.add(Variable(functionImplementation.getReceiver()!!, instance))
            functionImplementation.valueParameters
                .map { Variable(it.descriptor, stack.getVariableState(it.descriptor)) }
                .forEach { valueArguments.add(it) }
            return stack.newFrame(asSubFrame = true, initPool = valueArguments) {
                functionImplementation.interpret()
            }
        }
        return irFunction.body!!.interpret()
    }

    private suspend fun calculateOverridden(owner: IrSimpleFunction): ExecutionResult {
        val variableDescriptor = owner.getReceiver()!!
        val superQualifier = (stack.getVariableState(variableDescriptor) as? Complex)?.superClass
        if (superQualifier == null) {
            // superQualifier is null for exception state => find method in builtins
            return calculateBuiltIns(owner.getLastOverridden() as IrSimpleFunction)
        }
        val overridden = owner.overriddenSymbols.single()

        val valueArguments = mutableListOf<Variable>()
        valueArguments.add(Variable(overridden.owner.getReceiver()!!, superQualifier))
        owner.valueParameters.zip(overridden.owner.valueParameters)
            .map { Variable(it.second.descriptor, stack.getVariableState(it.first.descriptor)) }
            .forEach { valueArguments.add(it) }

        return stack.newFrame(asSubFrame = true, initPool = valueArguments) {
            val overriddenOwner = overridden.owner
            return@newFrame when {
                overriddenOwner.body != null -> overriddenOwner.interpret()
                superQualifier.superClass == null -> calculateBuiltIns(overriddenOwner)
                else -> calculateOverridden(overriddenOwner)
            }
        }
    }

    private suspend fun calculateBuiltIns(irFunction: IrFunction): ExecutionResult {
        val descriptor = irFunction.descriptor
        val methodName = when (val property = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol) {
            null -> descriptor.name.asString()
            else -> property.owner.name.asString()
        }
        val args = stack.getAll().map { it.state }

        val receiverType = descriptor.dispatchReceiverParameter?.type ?: descriptor.extensionReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + descriptor.valueParameters.map { it.original.type }
        val argsValues = args.map {
            when (it) {
                is Complex -> it.getOriginal()
                is Primitive<*> -> it.value
                else -> it // lambda can be used in built in calculation, for example, in null check
            }
        }
        val signature = CompileTimeFunction(methodName, argsType.map { it.toString() })

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

    private suspend fun calculateRangeTo(type: IrType): ExecutionResult {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)

        val primitiveValueParameters = stack.getAll().map { it.state as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(primitive.type))
        }

        val constructorValueParameters = constructor.valueParameters.map { it.descriptor }.zip(primitiveValueParameters)
        return stack.newFrame(initPool = constructorValueParameters.map { Variable(it.first, it.second) }) {
            constructorCall.interpret()
        }
    }

    private suspend fun interpretValueParameters(
        expression: IrFunctionAccessExpression, irFunction: IrFunction, pool: MutableList<Variable>
    ): ExecutionResult {
        // if irFunction is lambda and it has receiver, then first descriptor must be taken from extension receiver
        val receiverAsFirstArgument = when (expression.dispatchReceiver?.type?.isFunction()) {
            true -> listOfNotNull(irFunction.getExtensionReceiver())
            else -> listOf()
        }
        val valueParametersDescriptors = receiverAsFirstArgument + irFunction.descriptor.valueParameters

        val valueArguments = (0 until expression.valueArgumentsCount).map { expression.getValueArgument(it) }
        val defaultValues = expression.symbol.owner.valueParameters.map { it.defaultValue?.expression }

        return stack.newFrame(asSubFrame = true, initPool = pool) {
            for (i in valueArguments.indices) {
                (valueArguments[i] ?: defaultValues[i])?.interpret()?.check { return@newFrame it }
                    ?: stack.pushReturnValue(listOf<Any?>().toPrimitiveStateArray(expression.getVarargType(i)!!))

                with(Variable(valueParametersDescriptors[i], stack.popReturnValue())) {
                    stack.addVar(this)  //must add value argument in current stack because it can be used later as default argument
                    pool.add(this)
                }
            }
            Next
        }
    }

    private suspend fun interpretCall(expression: IrCall): ExecutionResult {
        val valueArguments = mutableListOf<Variable>()
        // dispatch receiver processing
        val rawDispatchReceiver = expression.dispatchReceiver
        rawDispatchReceiver?.interpret()?.check { return it }
        val dispatchReceiver = rawDispatchReceiver?.let { stack.popReturnValue() }

        // extension receiver processing
        val rawExtensionReceiver = expression.extensionReceiver
        rawExtensionReceiver?.interpret()?.check { return it }
        val extensionReceiver = rawExtensionReceiver?.let { stack.popReturnValue() }

        // find correct ir function
        val functionReceiver = dispatchReceiver?.getFunctionReceiver(expression.superQualifierSymbol?.owner)
        val irFunction = functionReceiver?.getIrFunction(expression.symbol.descriptor) ?: expression.symbol.owner

        // it is important firstly to add receiver, then arguments; this order is used in builtin method call
        irFunction.getDispatchReceiver()?.let { functionReceiver?.let { receiver -> valueArguments.add(Variable(it, receiver)) } }
        irFunction.getExtensionReceiver()?.let { extensionReceiver?.let { receiver -> valueArguments.add(Variable(it, receiver)) } }

        interpretValueParameters(expression, irFunction, valueArguments).check { return it }

        valueArguments.addAll(getTypeArguments(irFunction, expression) { stack.getVariableState(it) })
        if (dispatchReceiver is Complex) valueArguments.addAll(dispatchReceiver.typeArguments)
        if (extensionReceiver is Complex) valueArguments.addAll(extensionReceiver.typeArguments)

        val isLocal = (dispatchReceiver as? Complex)?.getOriginal()?.irClass?.isLocal ?: irFunction.isLocal
        if (isLocal) valueArguments.addAll(dispatchReceiver.extractNonLocalDeclarations())

        return stack.newFrame(asSubFrame = irFunction.isInline || irFunction.isLocal, initPool = valueArguments) {
            val isWrapper = dispatchReceiver is Wrapper && rawExtensionReceiver == null
            val isInterfaceDefaultMethod = irFunction.body != null && (irFunction.parent as? IrClass)?.isInterface == true
            return@newFrame when {
                isWrapper && !isInterfaceDefaultMethod -> (dispatchReceiver as Wrapper).getMethod(irFunction).invokeMethod(irFunction)
                irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction)
                irFunction.isAbstract() -> calculateAbstract(irFunction) //abstract check must be before fake overridden check
                irFunction.isFakeOverridden() -> calculateOverridden(irFunction as IrSimpleFunction)
                irFunction.body == null || dispatchReceiver is Primitive<*> -> calculateBuiltIns(irFunction)
                else -> irFunction.interpret()
            }
        }
    }

    private suspend fun interpretInstanceInitializerCall(call: IrInstanceInitializerCall): ExecutionResult {
        val irClass = call.classSymbol.owner

        // properties processing
        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        classProperties.forEach { property ->
            property.backingField?.initializer?.expression?.interpret()?.check { return it }
            val receiver = irClass.descriptor.thisAsReceiverParameter
            if (property.backingField?.initializer != null) {
                val receiverState = stack.getVariableState(receiver)
                val propertyState = Variable(property.backingField!!.descriptor, stack.popReturnValue())
                receiverState.setState(propertyState)
            }
        }

        // init blocks processing
        val anonymousInitializer = irClass.declarations.filterIsInstance<IrAnonymousInitializer>().filter { !it.isStatic }
        anonymousInitializer.forEach { init -> init.body.interpret().check { return it } }

        return Next
    }

    private suspend fun interpretConstructor(constructorCall: IrFunctionAccessExpression): ExecutionResult {
        val owner = constructorCall.symbol.owner
        val valueArguments = mutableListOf<Variable>()

        interpretValueParameters(constructorCall, owner, valueArguments).check { return it }

        val parent = owner.parent as IrClass
        val typeArguments = getTypeArguments(parent, constructorCall) { stack.getVariableState(it) } + stack.getAllTypeArguments()
        if (parent.hasAnnotation(evaluateIntrinsicAnnotation)) {
            return stack.newFrame(initPool = valueArguments) { Wrapper.getConstructorMethod(owner).invokeMethod(owner) }
                .apply { (stack.peekReturnValue() as? Wrapper)?.typeArguments?.addAll(typeArguments) } // can be exception
        }

        if (parent.defaultType.isArray() || parent.defaultType.isPrimitiveArray()) {
            // array constructor doesn't have body so must be treated separately
            return stack.newFrame(initPool = valueArguments) { handleIntrinsicMethods(owner) }
        }

        val state = Common(parent).apply { this.typeArguments.addAll(typeArguments) }
        if (parent.isLocal) state.fields.addAll(stack.getAll()) // TODO save only necessary declarations
        valueArguments.add(Variable(constructorCall.getThisAsReceiver(), state)) //used to set up fields in body
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

    private suspend fun interpretConstructorCall(constructorCall: IrConstructorCall): ExecutionResult {
        return interpretConstructor(constructorCall)
    }

    private suspend fun interpretEnumConstructorCall(enumConstructorCall: IrEnumConstructorCall): ExecutionResult {
        return interpretConstructor(enumConstructorCall)
    }

    private suspend fun interpretDelegatedConstructorCall(delegatingConstructorCall: IrDelegatingConstructorCall): ExecutionResult {
        if (delegatingConstructorCall.symbol.descriptor.containingDeclaration.defaultType == DefaultBuiltIns.Instance.anyType) {
            val anyAsStateObject = Common(irBuiltIns.anyClass.owner)
            stack.pushReturnValue(anyAsStateObject)
            return Next
        }

        return interpretConstructor(delegatingConstructorCall)
    }

    private suspend fun interpretConst(expression: IrConst<*>): ExecutionResult {
        fun getSignedType(unsignedClassName: String): IrType {
            return when (unsignedClassName) {
                "UByte" -> irBuiltIns.byteType
                "UShort" -> irBuiltIns.shortType
                "UInt" -> irBuiltIns.intType
                "ULong" -> irBuiltIns.longType
                else -> throw InterpreterException("Unsupported unsigned class $unsignedClassName")
            }
        }

        return if (UnsignedTypes.isUnsignedType(expression.type.toKotlinType())) {
            val unsignedClass = expression.type.classOrNull!!
            val constructor = unsignedClass.constructors.single().owner
            val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
            constructorCall.putValueArgument(0, expression.value.toIrConst(getSignedType(unsignedClass.owner.name.asString())))

            constructorCall.interpret()
        } else {
            stack.pushReturnValue(expression.toPrimitive())
            Next
        }
    }

    private suspend fun interpretStatements(statements: List<IrStatement>): ExecutionResult {
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

    private suspend fun interpretBlock(block: IrBlock): ExecutionResult {
        return stack.newFrame(asSubFrame = true) { interpretStatements(block.statements) }
    }

    private suspend fun interpretBody(body: IrBody): ExecutionResult {
        return stack.newFrame(asSubFrame = true) { interpretStatements(body.statements) }
    }

    private suspend fun interpretReturn(expression: IrReturn): ExecutionResult {
        expression.value.interpret().check { return it }
        return Return.addInfo(expression.returnTargetSymbol.descriptor.toString())
    }

    private suspend fun interpretWhile(expression: IrWhileLoop): ExecutionResult {
        while (true) {
            expression.condition.interpret().check { return it }
            if (stack.popReturnValue().asBooleanOrNull() != true) break
            expression.body?.interpret()?.check { return it }
        }
        return Next
    }

    private suspend fun interpretDoWhile(expression: IrDoWhileLoop): ExecutionResult {
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

    private suspend fun interpretWhen(expression: IrWhen): ExecutionResult {
        var executionResult: ExecutionResult = Next
        for (branch in expression.branches) {
            executionResult = branch.interpret().check { return it }
        }
        return executionResult
    }

    private suspend fun interpretBranch(expression: IrBranch): ExecutionResult {
        val executionResult = expression.condition.interpret().check { return it }
        if (stack.popReturnValue().asBooleanOrNull() == true) {
            expression.result.interpret().check { return it }
            return BreakWhen
        }
        return executionResult
    }

    private fun interpretBreak(breakStatement: IrBreak): ExecutionResult {
        return BreakLoop.addInfo(breakStatement.label ?: "")
    }

    private fun interpretContinue(continueStatement: IrContinue): ExecutionResult {
        return Continue.addInfo(continueStatement.label ?: "")
    }

    private suspend fun interpretSetField(expression: IrSetField): ExecutionResult {
        expression.value.interpret().check { return it }

        // receiver is null only for top level var, but it cannot be used in constexpr; corresponding check is on frontend
        val receiver = (expression.receiver as IrDeclarationReference).symbol.descriptor
        stack.getVariableState(receiver).setState(Variable(expression.symbol.owner.descriptor, stack.popReturnValue()))
        return Next
    }

    private suspend fun interpretGetField(expression: IrGetField): ExecutionResult {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol?.descriptor
        // receiver is null, for example, for top level fields
        val result = receiver?.let { stack.getVariableState(receiver).getState(expression.symbol.descriptor)?.copy() }
            ?: return (expression.symbol.owner.initializer?.expression?.interpret() ?: Next)
        stack.pushReturnValue(result)
        return Next
    }

    private fun interpretGetValue(expression: IrGetValue): ExecutionResult {
        stack.pushReturnValue(stack.getVariableState(expression.symbol.descriptor).copy())
        return Next
    }

    private suspend fun interpretVariable(expression: IrVariable): ExecutionResult {
        expression.initializer?.interpret()?.check { return it } ?: return Next
        stack.addVar(Variable(expression.descriptor, stack.popReturnValue()))
        return Next
    }

    private suspend fun interpretSetVariable(expression: IrSetVariable): ExecutionResult {
        expression.value.interpret().check { return it }

        if (stack.contains(expression.symbol.descriptor)) {
            val variable = stack.getVariableState(expression.symbol.descriptor)
            variable.setState(Variable(expression.symbol.descriptor, stack.popReturnValue()))
        } else {
            stack.addVar(Variable(expression.symbol.descriptor, stack.popReturnValue()))
        }
        return Next
    }

    private fun interpretGetObjectValue(expression: IrGetObjectValue): ExecutionResult {
        val owner = expression.symbol.owner
        val objectSignature = owner.fqNameWhenAvailable.toString()
        mapOfObjects[objectSignature]?.let { return Next.apply { stack.pushReturnValue(it) } }

        val objectState = when {
            owner.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getCompanionObject(owner)
            else -> Common(owner).apply { setSuperClassRecursive() } // TODO test type arguments
        }
        mapOfObjects[objectSignature] = objectState
        stack.pushReturnValue(objectState)
        return Next
    }

    private suspend fun interpretGetEnumValue(expression: IrGetEnumValue): ExecutionResult {
        val enumEntry = expression.symbol.owner
        val enumSignature = enumEntry.fqNameWhenAvailable.toString()
        mapOfEnums[enumSignature]?.let { return Next.apply { stack.pushReturnValue(it) } }

        val enumClass = enumEntry.symbol.owner.parentAsClass
        val valueOfFun = enumClass.declarations.single { it.nameForIrSerialization.asString() == "valueOf" } as IrFunction
        enumClass.declarations.filterIsInstance<IrEnumEntry>().forEach {
            val executionResult = when {
                enumClass.hasAnnotation(evaluateIntrinsicAnnotation) -> {
                    val enumEntryName = it.name.asString().toState(irBuiltIns.stringType)
                    val enumNameAsVariable = Variable(valueOfFun.valueParameters.first().descriptor, enumEntryName)
                    stack.newFrame(initPool = listOf(enumNameAsVariable)) { Wrapper.getEnumEntry(enumClass)!!.invokeMethod(valueOfFun) }
                }
                else -> interpretEnumEntry(it)
            }
            executionResult.check { result -> return result }
            mapOfEnums[it.fqNameWhenAvailable.toString()] = stack.popReturnValue() as Complex
        }

        stack.pushReturnValue(mapOfEnums[enumSignature]!!)
        return Next
    }

    private suspend fun interpretEnumEntry(enumEntry: IrEnumEntry): ExecutionResult {
        val enumClass = enumEntry.symbol.owner.parentAsClass
        val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()

        val enumSuperCall = (enumClass.primaryConstructor?.body?.statements?.firstOrNull() as? IrEnumConstructorCall)
        if (enumEntries.isNotEmpty() && enumSuperCall != null) {
            val valueArguments = listOf(
                enumEntry.name.asString().toIrConst(irBuiltIns.stringType), enumEntries.indexOf(enumEntry).toIrConst(irBuiltIns.intType)
            )
            enumSuperCall.mapValueParameters { valueArguments[it.index] }
        }

        val executionResult = enumEntry.initializerExpression?.interpret()?.check { return it }
        enumSuperCall?.mapValueParameters { null } // restore to null
        return executionResult ?: throw InterpreterException("Initializer at enum entry ${enumEntry.fqNameWhenAvailable} is null")
    }

    private suspend fun interpretTypeOperatorCall(expression: IrTypeOperatorCall): ExecutionResult {
        val executionResult = expression.argument.interpret().check { return it }
        val typeOperandDescriptor = expression.typeOperand.classifierOrFail.descriptor
        val typeOperandClass = expression.typeOperand.classOrNull?.owner ?: stack.getVariableState(typeOperandDescriptor).irClass

        when (expression.operator) {
            // coercion to unit means that return value isn't used
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> stack.popReturnValue()
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                if (!stack.peekReturnValue().irClass.isSubclassOf(typeOperandClass)) {
                    val convertibleClassName = stack.popReturnValue().irClass.fqNameWhenAvailable
                    throw ClassCastException("$convertibleClassName cannot be cast to ${typeOperandClass.fqNameWhenAvailable}")
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                if (!stack.peekReturnValue().irClass.isSubclassOf(typeOperandClass)) {
                    stack.popReturnValue()
                    stack.pushReturnValue(null.toState(irBuiltIns.nothingType))
                }
            }
            IrTypeOperator.INSTANCEOF -> {
                val isInstance = stack.popReturnValue().irClass.isSubclassOf(typeOperandClass)
                stack.pushReturnValue(isInstance.toState(irBuiltIns.nothingType))
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val isInstance = stack.popReturnValue().irClass.isSubclassOf(typeOperandClass)
                stack.pushReturnValue((!isInstance).toState(irBuiltIns.nothingType))
            }
            else -> TODO("${expression.operator} not implemented")
        }
        return executionResult
    }

    private suspend fun interpretVararg(expression: IrVararg): ExecutionResult {
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

        val array = when (expression.type.classifierOrFail.descriptor.name.asString()) {
            "UByteArray", "UShortArray", "UIntArray", "ULongArray" -> {
                val owner = expression.type.classOrNull!!.owner
                val constructor = owner.primaryConstructor!!
                val storageParameter = constructor.valueParameters.single()
                val primitiveArray = args.map { ((it as Common).fields.single().state as Primitive<*>).value }
                val unsignedArray = primitiveArray.toPrimitiveStateArray(storageParameter.type)
                Common(owner).apply {
                    setSuperClassRecursive()
                    fields.add(Variable(storageParameter.descriptor, unsignedArray))
                }
            }
            else -> args.toPrimitiveStateArray(expression.type)
        }
        stack.pushReturnValue(array)
        return Next
    }

    private suspend fun interpretSpreadElement(spreadElement: IrSpreadElement): ExecutionResult {
        return spreadElement.expression.interpret().check { return it }
    }

    private suspend fun interpretTry(expression: IrTry): ExecutionResult {
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

    private suspend fun interpretCatch(expression: IrCatch): ExecutionResult {
        val catchParameter = Variable(expression.parameter, stack.popReturnValue())
        return stack.newFrame(asSubFrame = true, initPool = listOf(catchParameter)) {
            expression.result.interpret()
        }
    }

    private suspend fun interpretThrow(expression: IrThrow): ExecutionResult {
        expression.value.interpret().check { return it }
        when (val exception = stack.popReturnValue()) {
            is Common -> stack.pushReturnValue(ExceptionState(exception, stack.getStackTrace()))
            is Wrapper -> stack.pushReturnValue(ExceptionState(exception, stack.getStackTrace()))
            is ExceptionState -> stack.pushReturnValue(exception)
            else -> throw InterpreterException("${exception::class} cannot be used as exception state")
        }
        return Exception
    }

    private suspend fun interpretStringConcatenation(expression: IrStringConcatenation): ExecutionResult {
        val result = StringBuilder()
        expression.arguments.forEach {
            it.interpret().check { executionResult -> return executionResult }
            result.append(
                when (val returnValue = stack.popReturnValue()) {
                    is Primitive<*> -> returnValue.value.toString()
                    is Wrapper -> returnValue.value.toString()
                    is Common -> {
                        val toStringFun = returnValue.getToStringFunction()
                        stack.newFrame(initPool = mutableListOf(Variable(toStringFun.getReceiver()!!, returnValue))) {
                            toStringFun.body?.let { toStringFun.interpret() } ?: calculateOverridden(toStringFun)
                        }.check { executionResult -> return executionResult }
                        stack.popReturnValue().asString()
                    }
                    else -> throw InterpreterException("$returnValue cannot be used in StringConcatenation expression")
                }
            )
        }

        stack.pushReturnValue(result.toString().toState(expression.type))
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

    private suspend fun interpretComposite(expression: IrComposite): ExecutionResult {
        return when (expression.origin) {
            IrStatementOrigin.DESTRUCTURING_DECLARATION -> interpretStatements(expression.statements)
            null -> interpretStatements(expression.statements) // is null for body of do while loop
            else -> TODO("${expression.origin} not implemented")
        }
    }
}