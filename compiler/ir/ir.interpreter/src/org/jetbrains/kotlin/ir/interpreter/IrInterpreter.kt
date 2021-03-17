/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.interpreter.builtins.evaluateIntrinsicAnnotation
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretBinaryFunction
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretTernaryFunction
import org.jetbrains.kotlin.ir.interpreter.builtins.interpretUnaryFunction
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterTimeOutError
import org.jetbrains.kotlin.ir.interpreter.intrinsics.IntrinsicEvaluator
import org.jetbrains.kotlin.ir.interpreter.proxy.CommonProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.proxy.wrap
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.Complex
import org.jetbrains.kotlin.ir.interpreter.state.ExceptionState
import org.jetbrains.kotlin.ir.interpreter.state.Primitive
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.state.asBoolean
import org.jetbrains.kotlin.ir.interpreter.state.isSubtypeOf
import org.jetbrains.kotlin.ir.interpreter.state.reflection.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KClassState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KFunctionState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KPropertyState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.KTypeState
import org.jetbrains.kotlin.ir.interpreter.state.reflection.ReflectionState
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.originalKotlinType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.lang.invoke.MethodHandle

internal interface Instruction {
    val element: IrElement?
}

internal class CompoundInstruction(override val element: IrElement?) : Instruction // must unwind first
internal class SimpleInstruction(override val element: IrElement) : Instruction   // must interpret as is
internal class CustomInstruction(val evaluate: () -> Unit) : Instruction {
    override val element: IrElement?
        get() = null
}

class IrInterpreter private constructor(
    private val bodyMap: Map<IdSignature, IrBody>,
    private val environment: IrInterpreterEnvironment
) {
    val irBuiltIns: IrBuiltIns
        get() = environment.irBuiltIns
    private val callStack: CallStack
        get() = environment.callStack
    private var commandCount = 0

    constructor(irBuiltIns: IrBuiltIns, bodyMap: Map<IdSignature, IrBody> = emptyMap()) :
            this(bodyMap, IrInterpreterEnvironment(irBuiltIns, CallStack()))

    private constructor(environment: IrInterpreterEnvironment, bodyMap: Map<IdSignature, IrBody> = emptyMap()) :
            this(bodyMap, environment)

    constructor(irModule: IrModuleFragment) : this(emptyMap(), IrInterpreterEnvironment(irModule))

    private fun incrementAndCheckCommands() {
        commandCount++
        if (commandCount >= IrInterpreterEnvironment.MAX_COMMANDS) InterpreterTimeOutError().handleUserException(environment)
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
            else -> defaultType
        }
    }

    private fun getUnitState(): State {
        return environment.mapOfObjects.getOrPut(irBuiltIns.unitClass) { Common(irBuiltIns.unitClass.owner) }
    }

    private fun Instruction.handle() {
        when (this) {
            is CompoundInstruction -> unfoldInstruction(this.element, environment)
            is SimpleInstruction -> interpret(this.element)
            is CustomInstruction -> this.evaluate()
        }
    }

    fun interpret(expression: IrExpression, file: IrFile? = null): IrExpression {
        commandCount = 0
        callStack.newFrame(expression, listOf(CompoundInstruction(expression)), file)

        while (!callStack.hasNoInstructions()) {
            callStack.popInstruction().handle()
            incrementAndCheckCommands()
        }

        return callStack.popState().toIrExpression(expression).apply { callStack.dropFrame() }
    }

    private fun withNewCallStack(block: IrInterpreter.() -> Any?): Any? {
        return with(IrInterpreter(environment.copyWithNewCallStack(), bodyMap)) {
            block()
        }
    }

    internal fun IrFunction.proxyInterpret(valueArguments: List<Variable>, expectedResultClass: Class<*> = Any::class.java): Any? {
        return withNewCallStack {
            callStack.newFrame(this@proxyInterpret, listOf(CompoundInstruction(this@proxyInterpret)))
            valueArguments.forEach { callStack.addVariable(it) }

            while (!callStack.hasNoInstructions()) {
                callStack.popInstruction().handle()
                incrementAndCheckCommands()
            }

            if (callStack.peekState() == null) callStack.pushState(getUnitState()) // TODO maybe move this logic to body/block
            callStack.popState().wrap(this@IrInterpreter, expectedResultClass).apply { callStack.dropFrame() }
        }
    }

    private fun interpret(element: IrElement) {
        when (element) {
            is IrSimpleFunction -> interpretFunction(element)
            is IrConstructor -> interpretConstructor(element)
            is IrCall -> interpretCall(element)
            is IrConstructorCall -> interpretConstructorCall(element)
            is IrEnumConstructorCall -> interpretEnumConstructorCall(element)
            is IrDelegatingConstructorCall -> interpretDelegatingConstructorCall(element)
            is IrValueParameter -> interpretValueParameter(element)
            is IrInstanceInitializerCall -> interpretInstanceInitializerCall(element)
            is IrField -> interpretField(element)
            is IrBlock -> callStack.dropSubFrame()
            is IrReturn -> interpretReturn(element)
            is IrSetField -> interpretSetField(element)
            is IrGetField -> interpretGetField(element)
            is IrGetObjectValue -> interpretGetObjectValue(element)
            is IrGetEnumValue -> interpretGetEnumValue(element)
            is IrEnumEntry -> interpretEnumEntry(element)
            is IrConst<*> -> interpretConst(element)
            is IrVariable -> callStack.addVariable(Variable(element.symbol, callStack.popState()))
            is IrSetValue -> callStack.getVariable(element.symbol).state = callStack.popState()
            is IrTypeOperatorCall -> interpretTypeOperatorCall(element)
            is IrBranch -> interpretBranch(element)
            is IrWhileLoop -> interpretWhile(element)
            is IrDoWhileLoop -> interpretDoWhile(element)
            is IrWhen -> callStack.dropSubFrame()
            is IrVararg -> interpretVararg(element)
            is IrTry -> interpretTry(element)
            is IrThrow -> interpretThrow(element)
            is IrStringConcatenation -> interpretStringConcatenation(element)
            is IrFunctionExpression -> interpretFunctionExpression(element)
            is IrFunctionReference -> interpretFunctionReference(element)
            is IrPropertyReference -> interpretPropertyReference(element)
            is IrClassReference -> interpretClassReference(element)
            else -> TODO("${element.javaClass} not supported for interpretation")
        }
    }

    private fun interpretFunction(function: IrSimpleFunction) {
        function.tryResetFunctionBody()
        if (function.checkCast(environment)) {
            callStack.dropFrameAndCopyResult()
        }
    }

    private fun MethodHandle?.invokeMethod(irFunction: IrFunction, args: List<State>) {
        this ?: return handleIntrinsicMethods(irFunction)
        val argsForMethodInvocation = irFunction.getArgsForMethodInvocation(this@IrInterpreter, this.type(), args)
        withExceptionHandler(environment) {
            val result = this.invokeWithArguments(argsForMethodInvocation)
            callStack.pushState(result.toState(result.getType(irFunction.returnType)))
        }
    }

    private fun handleIntrinsicMethods(irFunction: IrFunction) {
        IntrinsicEvaluator.unwindInstructions(irFunction, environment).forEach { callStack.addInstruction(it) }
    }

    private fun calculateBuiltIns(irFunction: IrFunction, args: List<State>) {
        val methodName = when (val property = (irFunction as? IrSimpleFunction)?.correspondingPropertySymbol) {
            null -> irFunction.name.asString()
            else -> property.owner.name.asString()
        }

        val receiverType = irFunction.dispatchReceiverParameter?.type
        val argsType = listOfNotNull(receiverType) + irFunction.valueParameters.map { it.type }
        val argsValues = args.map { it.wrap(this, calledFromBuiltIns = methodName !in setOf("plus", IrBuiltIns.OperatorNames.EQEQ)) }

        fun IrType.getOnlyName(): String {
            return when {
                this.originalKotlinType != null -> this.originalKotlinType.toString()
                this is IrSimpleType -> (this.classifierOrFail.owner as IrDeclarationWithName).name.asString() + (if (this.hasQuestionMark) "?" else "")
                else -> this.render()
            }
        }

        // TODO replace unary, binary, ternary functions with vararg
        withExceptionHandler(environment) {
            val result = when (argsType.size) {
                1 -> interpretUnaryFunction(methodName, argsType[0].getOnlyName(), argsValues[0])
                2 -> when (methodName) {
                    "rangeTo" -> return calculateRangeTo(irFunction.returnType, args)
                    else -> interpretBinaryFunction(
                        methodName, argsType[0].getOnlyName(), argsType[1].getOnlyName(), argsValues[0], argsValues[1]
                    )
                }
                3 -> interpretTernaryFunction(
                    methodName, argsType[0].getOnlyName(), argsType[1].getOnlyName(), argsType[2].getOnlyName(),
                    argsValues[0], argsValues[1], argsValues[2]
                )
                else -> throw InterpreterError("Unsupported number of arguments for invocation as builtin functions")
            }
            // TODO check "result is Unit"
            callStack.pushState(result.toState(result.getType(irFunction.returnType)))
        }
    }

    private fun calculateRangeTo(type: IrType, args: List<State>) {
        val constructor = type.classOrNull!!.owner.constructors.first()
        val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
        val constructorValueParameters = constructor.valueParameters.map { it.symbol }

        val primitiveValueParameters = args.map { it as Primitive<*> }
        primitiveValueParameters.forEachIndexed { index, primitive ->
            constructorCall.putValueArgument(index, primitive.value.toIrConst(constructorValueParameters[index].owner.type))
        }

        callStack.addInstruction(CompoundInstruction(constructorCall))
    }

    private fun interpretValueParameter(valueParameter: IrValueParameter) {
        val irFunction = valueParameter.parent as IrFunction
        fun isReceiver() = irFunction.dispatchReceiverParameter == valueParameter || irFunction.extensionReceiverParameter == valueParameter

        val result = callStack.popState()
        val state = when {
            // if vararg is empty
            result.isNull() && valueParameter.isVararg -> listOf<Any?>().toPrimitiveStateArray((result as Primitive<*>).type)
            else -> result
        }

        state.checkNullability(valueParameter.type, environment) {
            if (isReceiver()) return@checkNullability NullPointerException()
            val method = irFunction.getCapitalizedFileName() + "." + irFunction.fqNameWhenAvailable
            val parameter = valueParameter.name
            IllegalArgumentException("Parameter specified as non-null is null: method $method, parameter $parameter")
        } ?: return

        //must add value argument in current stack because it can be used later as default argument
        callStack.addVariable(Variable(valueParameter.symbol, state))
        callStack.pushState(state)
    }

    private fun interpretCall(call: IrCall) {
        // 1. load evaluated arguments from stack
        val valueArguments = call.symbol.owner.valueParameters.map { callStack.popState() }.reversed()
        val extensionReceiver = call.extensionReceiver?.let { callStack.popState() }
        var dispatchReceiver = call.dispatchReceiver?.let { callStack.popState() }

        // 2. get correct function for interpretation
        val irFunction = dispatchReceiver?.getIrFunctionByIrCall(call) ?: call.symbol.owner
        dispatchReceiver = when (irFunction.parent) {
            (dispatchReceiver as? Complex)?.superWrapperClass?.irClass -> dispatchReceiver.superWrapperClass
            else -> dispatchReceiver
        }

        callStack.dropSubFrame()
        callStack.newFrame(irFunction, listOf(SimpleInstruction(irFunction)))
        // TODO: if using KTypeState then it's class must be corresponding
        // `call.type` is used in check cast and emptyArray
        callStack.addVariable(Variable(irFunction.symbol, KTypeState(call.type, irBuiltIns.anyClass.owner)))

        // 3. store arguments in memory
        val args = mutableListOf<Variable>()
        irFunction.getDispatchReceiver()?.let { dispatchReceiver?.let { receiver -> args.add(Variable(it, receiver)) } }
        irFunction.getExtensionReceiver()?.let { args.add(Variable(it, extensionReceiver ?: valueArguments.first())) }
        // `shift` is used when extension receiver is actually a parameter in lambda
        val shift = if (irFunction.extensionReceiverParameter != null && extensionReceiver == null) 1 else 0
        irFunction.valueParameters.forEachIndexed { i, param -> args.add(Variable(param.symbol, valueArguments[i + shift])) }
        args.forEach { callStack.addVariable(it) }

        // 4. store reified type parameters
        irFunction.typeParameters.filter { it.isReified }
            .forEach { callStack.addVariable(Variable(it.symbol, KTypeState(call.getTypeArgument(it.index)!!, irBuiltIns.anyClass.owner))) }

        // 5. load up values onto stack
        if (dispatchReceiver == null && extensionReceiver == null && irFunction.isLocal) callStack.copyUpValuesFromPreviousFrame()
        if (dispatchReceiver is StateWithClosure) callStack.loadUpValues(dispatchReceiver)
        if (extensionReceiver is StateWithClosure) callStack.loadUpValues(extensionReceiver)

        // 6. load outer class object
        if (dispatchReceiver is Complex && irFunction.parentClassOrNull?.isInner == true) {
            generateSequence(dispatchReceiver.outerClass) { (it.state as? Complex)?.outerClass }.forEach { callStack.addVariable(it) }
        }

        val states = args.map { it.state }
        // inline only methods are not presented in lookup table, so must be interpreted instead of execution
        val isInlineOnly = irFunction.hasAnnotation(FqName("kotlin.internal.InlineOnly"))
        when {
            dispatchReceiver is Wrapper && !isInlineOnly -> dispatchReceiver.getMethod(irFunction).invokeMethod(irFunction, states)
            irFunction.hasAnnotation(evaluateIntrinsicAnnotation) -> Wrapper.getStaticMethod(irFunction).invokeMethod(irFunction, states)
            dispatchReceiver is KFunctionState && call.symbol.owner.name.asString() == "invoke" -> callStack.addInstruction(CompoundInstruction(irFunction))
            dispatchReceiver is ReflectionState -> Wrapper.getReflectionMethod(irFunction).invokeMethod(irFunction, states)
            dispatchReceiver is Primitive<*> -> calculateBuiltIns(irFunction, states) // 'is Primitive' check for js char, js long and get field for primitives
            irFunction.body is IrSyntheticBody -> handleIntrinsicMethods(irFunction)
            irFunction.body == null ->
                irFunction.trySubstituteFunctionBody() ?: irFunction.tryCalculateLazyConst() ?: calculateBuiltIns(irFunction, states)
            else -> callStack.addInstruction(CompoundInstruction(irFunction))
        }
    }

    private fun IrFunction.trySubstituteFunctionBody(): IrElement? {
        val signature = this.symbol.signature ?: return null
        this.body = bodyMap[signature] ?: return null
        callStack.addInstruction(CompoundInstruction(this))
        return body
    }

    private fun IrFunction.tryResetFunctionBody() {
        val signature = this.symbol.signature ?: return
        if (bodyMap[signature] != null) this.body = null
    }

    // TODO fix in FIR2IR; const val getter must have body with IrGetField node
    private fun IrFunction.tryCalculateLazyConst(): IrExpression? {
        if (this !is IrSimpleFunction) return null
        val expression = this.correspondingPropertySymbol?.owner?.backingField?.initializer?.expression
        return expression?.apply { callStack.addInstruction(CompoundInstruction(this)) }
    }

    private fun interpretInstanceInitializerCall(call: IrInstanceInitializerCall) {
        val irClass = call.classSymbol.owner

        val classProperties = irClass.declarations.filterIsInstance<IrProperty>()
        classProperties.forEach { property ->
            property.backingField?.initializer?.expression ?: return@forEach
            val receiver = irClass.thisReceiver!!.symbol
            if (property.backingField?.initializer != null) {
                val receiverState = callStack.getVariable(receiver).state
                val propertyVar = Variable(property.symbol, callStack.popState())
                receiverState.setField(propertyVar)
            }
        }
    }

    private fun interpretField(field: IrField) {
        val irClass = field.parentAsClass
        val receiver = irClass.thisReceiver!!.symbol
        val receiverState = callStack.getVariable(receiver).state
        receiverState.setField(Variable(field.correspondingPropertySymbol!!, callStack.popState()))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun interpretConstructor(constructor: IrConstructor) {
        callStack.dropFrameAndCopyResult()
    }

    private fun interpretConstructorCall(constructorCall: IrFunctionAccessExpression) {
        val valueArguments = constructorCall.symbol.owner.valueParameters.map { callStack.popState() }.reversed()
        val constructor = constructorCall.symbol.owner
        val irClass = constructor.parentAsClass
        val objectVar = callStack.getVariable(constructorCall.getThisReceiver())
        if (irClass.isLocal) callStack.storeUpValues(objectVar.state as StateWithClosure)
        val outerClass = if (irClass.isInner) callStack.popState() else null

        callStack.dropSubFrame() // TODO check that data stack is empty
        callStack.newFrame(constructor, listOf(SimpleInstruction(constructor)))
        callStack.addVariable(objectVar)
        constructor.valueParameters.forEachIndexed { i, param -> callStack.addVariable(Variable(param.symbol, valueArguments[i])) }
        if (irClass.isLocal) callStack.loadUpValues(objectVar.state as StateWithClosure)

        if (outerClass != null) {
            val outerClassVar = Variable(irClass.parentAsClass.thisReceiver!!.symbol, outerClass)
            (objectVar.state as Complex).outerClass = outerClassVar
            // used in case when inner class has inner super class
            callStack.addVariable(Variable(constructor.dispatchReceiverParameter!!.symbol, outerClass))
            // used to get information from outer class
            callStack.addVariable(outerClassVar)
        }

        val superReceiver = when (val irStatement = constructor.body?.statements?.get(0)) {
            null -> null // for jvm
            is IrTypeOperatorCall -> (irStatement.argument as IrFunctionAccessExpression).getThisReceiver() // for enums
            is IrFunctionAccessExpression -> irStatement.getThisReceiver()
            is IrBlock -> (irStatement.statements.last() as IrFunctionAccessExpression).getThisReceiver()
            else -> TODO("${irStatement::class.java} is not supported as first statement in constructor call")
        }
        superReceiver?.let { callStack.addVariable(Variable(it, objectVar.state)) }

        when {
            irClass.hasAnnotation(evaluateIntrinsicAnnotation) || irClass.fqNameWhenAvailable!!.startsWith(Name.identifier("java")) -> {
                Wrapper.getConstructorMethod(constructor).invokeMethod(constructor, valueArguments)
                if (constructorCall !is IrConstructorCall) (objectVar.state as Common).superWrapperClass = callStack.popState() as Wrapper
            }
            irClass.defaultType.isArray() || irClass.defaultType.isPrimitiveArray() -> {
                // array constructor doesn't have body so must be treated separately
                callStack.addVariable(Variable(constructor.symbol, KTypeState(constructorCall.type, irBuiltIns.anyClass.owner)))
                handleIntrinsicMethods(constructor)
            }
            irClass.defaultType.isUnsignedType() && valueArguments.size == 1 && constructor.valueParameters.size == 1 -> {
                callStack.pushState(objectVar.state.apply { fields.add(valueArguments.single()) })
                callStack.addInstruction(SimpleInstruction(constructor))
            }
            else -> {
                callStack.pushState(objectVar.state)
                callStack.addInstruction(CompoundInstruction(constructor))
            }
        }

    }

    private fun interpretDelegatingConstructorCall(constructorCall: IrDelegatingConstructorCall) {
        if (constructorCall.symbol.owner.parent == irBuiltIns.anyClass.owner) return callStack.dropSubFrame()
        interpretConstructorCall(constructorCall)
    }

    private fun interpretEnumConstructorCall(constructorCall: IrEnumConstructorCall) {
        interpretConstructorCall(constructorCall)
    }

    private fun interpretConst(expression: IrConst<*>) {
        fun getSignedType(unsignedType: IrType): IrType? = when (unsignedType.getUnsignedType()) {
            UnsignedType.UBYTE -> irBuiltIns.byteType
            UnsignedType.USHORT -> irBuiltIns.shortType
            UnsignedType.UINT -> irBuiltIns.intType
            UnsignedType.ULONG -> irBuiltIns.longType
            else -> null
        }

        val signedType = getSignedType(expression.type)
        if (signedType != null) {
            val unsignedClass = expression.type.classOrNull!!
            val constructor = unsignedClass.constructors.single().owner
            val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
            constructorCall.putValueArgument(0, expression.value.toIrConst(signedType))

            return callStack.addInstruction(CompoundInstruction(constructorCall))
        }
        callStack.pushState(expression.toPrimitive())
    }

    private fun interpretReturn(expression: IrReturn) {
        val function = expression.returnTargetSymbol.owner as? IrFunction
        function?.tryResetFunctionBody()
        if (function.checkCast(environment)) {
            callStack.returnFromFrameWithResult(expression)
        }
    }

    private fun interpretWhile(loop: IrWhileLoop) {
        val result = callStack.popState().asBoolean()
        callStack.dropSubFrame()
        if (result) {
            callStack.newSubFrame(
                loop,
                listOf(CompoundInstruction(loop.body), CompoundInstruction(loop.condition), SimpleInstruction(loop))
            )
        }
    }

    private fun interpretDoWhile(loop: IrDoWhileLoop) {
        val result = callStack.popState().asBoolean()
        callStack.dropSubFrame()
        if (result) {
            callStack.newSubFrame(
                loop,
                listOf(CompoundInstruction(loop.body), CompoundInstruction(loop.condition), SimpleInstruction(loop))
            )
        }
    }

    private fun interpretBranch(branch: IrBranch) {
        val result = callStack.popState().asBoolean()
        if (result) {
            callStack.dropSubFrame()
            callStack.addInstruction(CompoundInstruction(branch.result))
        }
    }

    private fun interpretSetField(expression: IrSetField) {
        val receiver = (expression.receiver as IrDeclarationReference).symbol
        val propertySymbol = expression.symbol.owner.correspondingPropertySymbol!!
        callStack.getVariable(receiver).apply { this.state.setField(Variable(propertySymbol, callStack.popState())) }
    }

    private fun interpretGetField(expression: IrGetField) {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol
        val field = expression.symbol.owner
        // for java static variables
        when {
            field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && field.isStatic -> {
                when (val initializerExpression = field.initializer?.expression) {
                    is IrConst<*> -> callStack.addInstruction(SimpleInstruction(initializerExpression))
                    else -> callStack.pushState(Wrapper.getStaticGetter(field)!!.invokeWithArguments().toState(field.type))
                }
            }
            field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD && field.correspondingPropertySymbol?.owner?.isConst == true -> {
                callStack.addInstruction(CompoundInstruction(field.initializer?.expression))
            }
            // receiver is null, for example, for top level fields
            expression.receiver.let { it == null || (it.type.classifierOrNull?.owner as? IrClass)?.isObject == true } -> {
                val propertyOwner = field.correspondingPropertySymbol?.owner
                val isConst = propertyOwner?.isConst == true || propertyOwner?.backingField?.initializer?.expression is IrConst<*>
                assert(isConst) { "Cannot interpret get method on top level non const properties" }
                callStack.addInstruction(CompoundInstruction(expression.symbol.owner.initializer?.expression))
            }
            else -> {
                val result = callStack.getVariable(receiver!!).state.getState(field.correspondingPropertySymbol!!)
                callStack.pushState(result!!)
            }
        }
    }

    private fun interpretGetObjectValue(expression: IrGetObjectValue) {
        callStack.dropSubFrame()

        val objectClass = expression.symbol.owner
        if (objectClass.hasAnnotation(evaluateIntrinsicAnnotation)) {
            val result = Wrapper.getCompanionObject(objectClass)
            environment.mapOfObjects[expression.symbol] = result
            callStack.pushState(result)
        }
    }

    private fun interpretGetEnumValue(expression: IrGetEnumValue) {
        callStack.pushState(environment.mapOfEnums[expression.symbol]!!)
    }

    private fun interpretEnumEntry(enumEntry: IrEnumEntry) {
        val enumClass = enumEntry.symbol.owner.parentAsClass

        when {
            enumClass.hasAnnotation(evaluateIntrinsicAnnotation) -> {
                val enumEntryName = enumEntry.name.asString().toState(environment.irBuiltIns.stringType)
                val valueOfFun = enumClass.declarations.single { it.nameForIrSerialization.asString() == "valueOf" } as IrFunction
                Wrapper.getEnumEntry(enumClass)!!.invokeMethod(valueOfFun, listOf(enumEntryName))
                environment.mapOfEnums[enumEntry.symbol] = callStack.popState() as Complex
            }
            else -> {
                val enumSuperCall = (enumClass.primaryConstructor?.body?.statements?.firstOrNull() as? IrEnumConstructorCall)
                enumSuperCall?.apply { (0 until this.valueArgumentsCount).forEach { putValueArgument(it, null) } } // restore to null
                callStack.dropSubFrame()
            }
        }
    }

    private fun interpretTypeOperatorCall(expression: IrTypeOperatorCall) {
        val typeClassifier = expression.typeOperand.classifierOrFail
        val isReified = (typeClassifier.owner as? IrTypeParameter)?.isReified == true
        val isErased = typeClassifier.owner is IrTypeParameter && !isReified
        val typeOperand = if (isReified) (callStack.getVariable(typeClassifier).state as KTypeState).irType else expression.typeOperand

        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                callStack.popState()
                // callStack.pushState(getUnitState()) TODO find real use cases for this
            }
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                if (!isErased && !callStack.peekState()!!.isSubtypeOf(typeOperand)) {
                    val convertibleClassName = callStack.popState().irClass.fqNameWhenAvailable
                    ClassCastException("$convertibleClassName cannot be cast to ${typeOperand.render()}").handleUserException(environment)
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                if (!isErased && !callStack.peekState()!!.isSubtypeOf(typeOperand)) {
                    callStack.popState()
                    callStack.pushState(null.toState(irBuiltIns.nothingNType))
                }
            }
            IrTypeOperator.INSTANCEOF -> {
                val isInstance = callStack.popState().isSubtypeOf(typeOperand) || isErased
                callStack.pushState(isInstance.toState(irBuiltIns.booleanType))
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val isInstance = callStack.popState().isSubtypeOf(typeOperand) || isErased
                callStack.pushState((!isInstance).toState(irBuiltIns.booleanType))
            }
            IrTypeOperator.IMPLICIT_NOTNULL -> {

            }
            else -> TODO("${expression.operator} not implemented")
        }
    }

    private fun interpretVararg(expression: IrVararg) {
        fun arrayToList(value: Any?): List<Any?> {
            return when (value) {
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
            }.reversed()
        }

        // TODO use wrap???
        val args = expression.elements.flatMap {
            return@flatMap when (val result = callStack.popState()) {
                is Wrapper -> listOf(result.value)
                is Primitive<*> -> when {
                    expression.varargElementType.isArray() || expression.varargElementType.isPrimitiveArray() -> listOf(result)
                    else -> arrayToList(result.value)
                }
                is Common -> when {
                    result.irClass.defaultType.isUnsignedArray() -> arrayToList((result.fields.single().state as Primitive<*>).value)
                    else -> listOf(result.asProxy(this))
                }
                else -> listOf(result)
            }
        }.reversed()

        val array = when {
            expression.type.isUnsignedArray() -> {
                val owner = expression.type.classOrNull!!.owner
                val storageProperty = owner.declarations.filterIsInstance<IrProperty>().first { it.name.asString() == "storage" }
                val primitiveArray = args.map {
                    when (it) {
                        is Proxy -> (it.state.fields.single().state as Primitive<*>).value  // is unsigned number
                        else -> it                                                          // is primitive number
                    }
                }
                val unsignedArray = primitiveArray.toPrimitiveStateArray(storageProperty.backingField!!.type)
                Common(owner).apply { fields.add(Variable(storageProperty.symbol, unsignedArray)) }
            }
            else -> args.toPrimitiveStateArray(expression.type)
        }
        callStack.pushState(array)
    }

    private fun interpretTry(element: IrTry) {
        val possibleException = callStack.peekState()
        callStack.dropSubFrame()
        if (possibleException is ExceptionState) {
            // after evaluation of finally, check that there are not unhandled exceptions left
            val checkUnhandledException = fun() {
                callStack.pushState(possibleException)
                callStack.dropFramesUntilTryCatch()
            }
            callStack.addInstruction(CustomInstruction(checkUnhandledException))
        }
        callStack.addInstruction(CompoundInstruction(element.finallyExpression))
    }

    private fun interpretThrow(expression: IrThrow) {
        val exception = callStack.popState()
        callStack.newSubFrame(expression, listOf()) // temporary frame to get correct stack trace
        when (exception) {
            is Common -> callStack.pushState(ExceptionState(exception, callStack.getStackTrace()))
            is Wrapper -> callStack.pushState(ExceptionState(exception, callStack.getStackTrace()))
            is ExceptionState -> callStack.pushState(exception)
            else -> throw InterpreterError("${exception::class} cannot be used as exception state")
        }
        callStack.dropFramesUntilTryCatch()
    }

    private fun interpretStringConcatenation(expression: IrStringConcatenation) {
        val result = mutableListOf<String>()
        repeat(expression.arguments.size) {
            result += when (val state = callStack.popState()) {
                is Primitive<*> -> state.value.toString()
                is Wrapper -> state.value.toString()
                else -> state.toString()
            }
        }

        callStack.dropSubFrame()
        callStack.pushState(result.reversed().joinToString(separator = "").toState(expression.type))
    }

    private fun interpretFunctionExpression(expression: IrFunctionExpression) {
        val function = KFunctionState(expression.function, expression.type.classOrNull!!.owner)
        if (expression.function.isLocal) callStack.storeUpValues(function)
        callStack.pushState(function)
    }

    private fun interpretFunctionReference(reference: IrFunctionReference) {
        val function = KFunctionState(reference)
        val irFunction = function.irFunction

        val extensionReceiver = reference.extensionReceiver?.let { callStack.popState() }
        val dispatchReceiver = reference.dispatchReceiver?.let { callStack.popState() }

        callStack.dropSubFrame()
        irFunction.getDispatchReceiver()?.let { dispatchReceiver?.let { receiver -> function.fields.add(Variable(it, receiver)) } }
        irFunction.getExtensionReceiver()?.let { extensionReceiver?.let { receiver -> function.fields.add(Variable(it, receiver)) } }

        if (irFunction.isLocal) callStack.storeUpValues(function)
        callStack.pushState(function)
    }

    private fun interpretPropertyReference(propertyReference: IrPropertyReference) {
        val dispatchReceiver = propertyReference.dispatchReceiver?.let { callStack.popState() }
        // it is impossible to get KProperty2 through ::, so extension receiver is always null
        val propertyState = KPropertyState(propertyReference, dispatchReceiver)
        callStack.pushState(propertyState)
    }

    private fun interpretClassReference(classReference: IrClassReference) {
        when (classReference.symbol) {
            is IrTypeParameterSymbol -> { // reified
                val kTypeState = callStack.getVariable(classReference.symbol).state as KTypeState
                callStack.pushState(KClassState(kTypeState.irType.classOrNull!!.owner, classReference.type.classOrNull!!.owner))
            }
            else -> callStack.pushState(KClassState(classReference))
        }
    }
}
