/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter

import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterError
import org.jetbrains.kotlin.ir.interpreter.exceptions.InterpreterTimeOutError
import org.jetbrains.kotlin.ir.interpreter.exceptions.handleUserException
import org.jetbrains.kotlin.ir.interpreter.exceptions.verify
import org.jetbrains.kotlin.ir.interpreter.proxy.CommonProxy.Companion.asProxy
import org.jetbrains.kotlin.ir.interpreter.proxy.Proxy
import org.jetbrains.kotlin.ir.interpreter.stack.CallStack
import org.jetbrains.kotlin.ir.interpreter.stack.Variable
import org.jetbrains.kotlin.ir.interpreter.state.*
import org.jetbrains.kotlin.ir.interpreter.state.reflection.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*

internal interface Instruction {
    val element: IrElement?
}

internal class CompoundInstruction(override val element: IrElement?) : Instruction // must unwind first
internal class SimpleInstruction(override val element: IrElement) : Instruction   // must interpret as is
internal class CustomInstruction(val evaluate: () -> Unit) : Instruction {
    override val element: IrElement?
        get() = null
}

class IrInterpreter(internal val environment: IrInterpreterEnvironment, internal val bodyMap: Map<IdSignature, IrBody>) {
    val irBuiltIns: IrBuiltIns
        get() = environment.irBuiltIns
    private val callStack: CallStack
        get() = environment.callStack
    private val callInterceptor: CallInterceptor = DefaultCallInterceptor(this)
    private var commandCount = 0

    constructor(irBuiltIns: IrBuiltIns, bodyMap: Map<IdSignature, IrBody> = emptyMap()) :
            this(IrInterpreterEnvironment(irBuiltIns), bodyMap)

    constructor(irModule: IrModuleFragment) : this(IrInterpreterEnvironment(irModule), emptyMap())

    private fun incrementAndCheckCommands() {
        commandCount++
        if (commandCount >= environment.configuration.maxCommands) InterpreterTimeOutError().handleUserException(environment)
    }

    private fun getUnitState(): State = environment.mapOfObjects[irBuiltIns.unitClass]!!

    private fun Instruction.handle() {
        when (this) {
            is CompoundInstruction -> unfoldInstruction(this.element, environment)
            is SimpleInstruction -> interpret(this.element).also { incrementAndCheckCommands() }
            is CustomInstruction -> this.evaluate()
        }
    }

    fun interpret(expression: IrExpression, file: IrFile? = null): IrExpression {
        commandCount = 0
        callStack.newFrame(expression, file)
        callStack.addInstruction(CompoundInstruction(expression))

        while (!callStack.hasNoInstructions()) {
            callStack.popInstruction().handle()
        }

        return callStack.popState().toIrExpression(expression).apply { callStack.dropFrame() }
    }

    internal fun withNewCallStack(call: IrCall, init: IrInterpreter.() -> Any?): State {
        return with(IrInterpreter(environment.copyWithNewCallStack(), bodyMap)) {
            callStack.newFrame(call.symbol.owner)
            init()

            while (!callStack.hasNoInstructions()) {
                callStack.popInstruction().handle()
            }

            callStack.popState().apply { callStack.dropFrame() }
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
            is IrField -> interpretField(element)
            is IrBody -> interpretBody(element)
            is IrBlock -> interpretBlock(element)
            is IrReturn -> interpretReturn(element)
            is IrSetField -> interpretSetField(element)
            is IrGetField -> interpretGetField(element)
            is IrGetObjectValue -> interpretGetObjectValue(element)
            is IrGetEnumValue -> interpretGetEnumValue(element)
            is IrEnumEntry -> interpretEnumEntry(element)
            is IrConst<*> -> interpretConst(element)
            is IrVariable -> callStack.addVariable(Variable(element.symbol, callStack.popState()))
            is IrSetValue -> callStack.setState(element.symbol, callStack.popState())
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
            is IrGetClass -> interpretGetClass(element)
            else -> TODO("${element.javaClass} not supported for interpretation")
        }
    }

    private fun IrFunction.tryResetFunctionBody() {
        val signature = this.symbol.signature ?: return
        if (bodyMap[signature] != null) this.body = null
    }

    private fun interpretFunction(function: IrSimpleFunction) {
        function.tryResetFunctionBody()
        if (function.checkCast(environment)) callStack.dropFrameAndCopyResult()
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

        callStack.pushState(state)
    }

    private fun interpretCall(call: IrCall) {
        fun State?.getThisOrSuperReceiver(irFunction: IrFunction): State? {
            // check if this function must be handled by super wrapper object
            if (this !is Common || this.superWrapperClass == null || irFunction.parent !is IrClass) return this
            if (superWrapperClass!!.irClass.isSubclassOf(irFunction.parentAsClass)) return superWrapperClass
            return this
        }

        val owner = call.symbol.owner
        // 1. load evaluated arguments from stack
        val valueArguments = owner.valueParameters.map { callStack.popState() }.reversed()
        val extensionReceiver = owner.getExtensionReceiver()?.let { callStack.popState() }
        val dispatchReceiver = owner.getDispatchReceiver()?.let { callStack.popState() }

        // 2. get correct function for interpretation
        val irFunction = dispatchReceiver?.getIrFunctionByIrCall(call) ?: call.symbol.owner
        val args = listOfNotNull(dispatchReceiver.getThisOrSuperReceiver(irFunction), extensionReceiver) + valueArguments

        // 3. evaluate reified type arguments; must do it here, before new frame, because outer type arguments can be loaded at this point
        val reifiedTypeArguments = irFunction.typeParameters.filter { it.isReified }
            .map {
                val reifiedType = call.getTypeArgument(it.index)!!.getTypeIfReified(callStack)
                Variable(it.symbol, KTypeState(reifiedType, irBuiltIns.anyClass.owner))
            }

        callStack.newFrame(irFunction)
        callStack.addInstruction(SimpleInstruction(irFunction))

        // 4. load up values onto stack; do it at first to set low priority of these variables
        if (dispatchReceiver is StateWithClosure) callStack.loadUpValues(dispatchReceiver)
        if (extensionReceiver is StateWithClosure) callStack.loadUpValues(extensionReceiver)
        if (irFunction.isLocal) callStack.copyUpValuesFromPreviousFrame()

        // 5. store arguments in memory (remap args on actual names)
        irFunction.getDispatchReceiver()?.let { dispatchReceiver?.let { receiver -> callStack.addVariable(Variable(it, receiver)) } }
        irFunction.getExtensionReceiver()?.let { callStack.addVariable(Variable(it, extensionReceiver ?: callStack.getState(it))) }
        irFunction.valueParameters.forEachIndexed { i, param -> callStack.addVariable(Variable(param.symbol, valueArguments[i])) }
        // TODO: if using KTypeState then it's class must be corresponding
        // `call.type` is used in check cast and emptyArray
        callStack.addVariable(Variable(irFunction.symbol, KTypeState(call.type, irBuiltIns.anyClass.owner)))

        // 6. store reified type parameters
        reifiedTypeArguments.forEach { callStack.addVariable(it) }

        // 7. load outer class object
        if (dispatchReceiver is Complex && irFunction.parentClassOrNull?.isInner == true) dispatchReceiver.loadOuterClassesInto(callStack)

        callInterceptor.interceptCall(call, irFunction, args) {
            callStack.addInstruction(CompoundInstruction(irFunction))
        }
    }

    private fun interpretField(field: IrField) {
        val irClass = field.parentAsClass
        val receiver = irClass.thisReceiver!!.symbol
        val receiverState = callStack.getState(receiver)
        receiverState.setField(Variable(field.correspondingPropertySymbol!!, callStack.popState()))
    }

    @Suppress("UNUSED_PARAMETER")
    private fun interpretBody(body: IrBody) {
        if (callStack.peekState() == null) callStack.pushState(getUnitState()) // implicit Unit result
    }

    @Suppress("UNUSED_PARAMETER")
    private fun interpretBlock(block: IrBlock) {
        callStack.dropSubFrame()
        if (callStack.peekState() == null) callStack.pushState(getUnitState()) // implicit Unit result
    }

    @Suppress("UNUSED_PARAMETER")
    private fun interpretConstructor(constructor: IrConstructor) {
        callStack.pushState(callStack.getState(constructor.parentAsClass.thisReceiver!!.symbol))
        callStack.dropFrameAndCopyResult()
    }

    private fun interpretConstructorCall(constructorCall: IrFunctionAccessExpression) {
        val constructor = constructorCall.symbol.owner
        val valueArguments = constructor.valueParameters.map { callStack.popState() }.reversed()
        val irClass = constructor.parentAsClass
        val receiverSymbol = constructor.dispatchReceiverParameter?.symbol

        // this check is used to be sure that object will be created once
        val objectState = when (constructorCall) {
            !is IrConstructorCall -> callStack.getState(constructorCall.getThisReceiver())
            else -> (if (irClass.isSubclassOfThrowable()) ExceptionState(irClass, callStack.getStackTrace()) else Common(irClass))
                .apply {
                    // must set object's state here, before actual initialization, to avoid cyclic evaluation
                    if (irClass.isObject) environment.mapOfObjects[irClass.symbol] = this
                }
        }

        if (irClass.isLocal) callStack.storeUpValues(objectState as StateWithClosure)
        val outerClass = receiverSymbol?.let { callStack.popState() }

        callStack.newFrame(constructor)
        callStack.addInstruction(SimpleInstruction(constructor))
        if (irClass.isLocal) callStack.loadUpValues(objectState as StateWithClosure)

        callStack.addVariable(Variable(constructorCall.getThisReceiver(), objectState))
        constructor.valueParameters.forEachIndexed { i, param -> callStack.addVariable(Variable(param.symbol, valueArguments[i])) }

        val superReceiver = when (val irStatement = constructor.body?.statements?.get(0)) {
            null -> null // for jvm
            is IrTypeOperatorCall -> (irStatement.argument as IrFunctionAccessExpression).getThisReceiver() // for enums
            is IrFunctionAccessExpression -> irStatement.getThisReceiver()
            is IrBlock -> (irStatement.statements.last() as IrFunctionAccessExpression).getThisReceiver()
            else -> TODO("${irStatement::class.java} is not supported as first statement in constructor call")
        }
        superReceiver?.let { callStack.addVariable(Variable(it, objectState)) }

        if (outerClass != null) {
            val outerClassVar = Variable(irClass.parentAsClass.thisReceiver!!.symbol, outerClass)
            (objectState as Complex).outerClass = outerClassVar
            if (superReceiver?.owner?.type != receiverSymbol.owner.type) {
                // This check is needed to test that this inner class is not subclass of its outer.
                // If it is true and if we add the next symbol, it will interfere with super symbol in memory.
                // In other case, we need this variable when inner class has inner super class.
                callStack.addVariable(Variable(receiverSymbol, outerClass))
                // used to get information from outer class
                objectState.loadOuterClassesInto(callStack, constructorCall.getThisReceiver())
            }
        }

        callInterceptor.interceptConstructor(constructorCall, valueArguments) {
            callStack.addInstruction(CompoundInstruction(constructor))
        }
    }

    private fun interpretDelegatingConstructorCall(constructorCall: IrDelegatingConstructorCall) {
        if (constructorCall.symbol.owner.parent == irBuiltIns.anyClass.owner) return
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
            callStack.newSubFrame(loop)
            callStack.addInstruction(SimpleInstruction(loop))
            callStack.addInstruction(CompoundInstruction(loop.condition))
            callStack.addInstruction(CompoundInstruction(loop.body))
        }
    }

    private fun interpretDoWhile(loop: IrDoWhileLoop) {
        val result = callStack.popState().asBoolean()
        callStack.dropSubFrame()
        if (result) {
            callStack.newSubFrame(loop)
            callStack.addInstruction(SimpleInstruction(loop))
            callStack.addInstruction(CompoundInstruction(loop.condition))
            callStack.addInstruction(CompoundInstruction(loop.body))
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
        callStack.getState(receiver).apply { this.setField(Variable(propertySymbol, callStack.popState())) }
    }

    private fun interpretGetField(expression: IrGetField) {
        val receiver = (expression.receiver as? IrDeclarationReference)?.symbol
        val field = expression.symbol.owner
        when {
            field.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && field.isStatic -> {
                // for java static variables
                when (val initializerExpression = field.initializer?.expression) {
                    is IrConst<*> -> callStack.addInstruction(SimpleInstruction(initializerExpression))
                    else -> callInterceptor.interceptJavaStaticField(expression)
                }
            }
            field.origin == IrDeclarationOrigin.PROPERTY_BACKING_FIELD && field.correspondingPropertySymbol?.owner?.isConst == true -> {
                callStack.addInstruction(CompoundInstruction(field.initializer?.expression))
            }
            expression.accessesTopLevelOrObjectField() -> {
                val propertyOwner = field.correspondingPropertySymbol?.owner
                val isConst = propertyOwner?.isConst == true || propertyOwner?.backingField?.initializer?.expression is IrConst<*>
                verify(isConst) { "Cannot interpret get method on top level non const properties" }
                callStack.addInstruction(CompoundInstruction(field.initializer?.expression))
            }
            else -> {
                val result = callStack.getState(receiver!!).getField(field.correspondingPropertySymbol!!)
                callStack.pushState(result!!)
            }
        }
    }

    private fun interpretGetObjectValue(expression: IrGetObjectValue) {
        callInterceptor.interceptGetObjectValue(expression) {
            val objectClass = expression.symbol.owner

            // non compile time objects can be used in interpreter, for example, to evaluate const properties or in tests
            val buildObject = objectClass.hasAnnotation(compileTimeAnnotation) || environment.configuration.createNonCompileTimeObjects
            if (objectClass.constructors.none() || !buildObject) {
                val state = Common(objectClass)
                environment.mapOfObjects[objectClass.symbol] = state
                return@interceptGetObjectValue callStack.pushState(state)
            }

            val constructor = objectClass.constructors.first()
            val constructorCall = IrConstructorCallImpl.fromSymbolOwner(constructor.returnType, constructor.symbol)
            callStack.addInstruction(CompoundInstruction(constructorCall))
        }
    }

    private fun interpretGetEnumValue(expression: IrGetEnumValue) {
        callStack.pushState(environment.mapOfEnums[expression.symbol]!!)
    }

    private fun interpretEnumEntry(enumEntry: IrEnumEntry) {
        callInterceptor.interceptEnumEntry(enumEntry) {
            val enumClass = enumEntry.symbol.owner.parentAsClass
            val enumEntries = enumClass.declarations.filterIsInstance<IrEnumEntry>()
            val enumSuperCall = (enumClass.primaryConstructor?.body?.statements?.firstOrNull() as? IrEnumConstructorCall)

            val cleanEnumSuperCall = fun() {
                enumSuperCall?.apply { (0 until this.valueArgumentsCount).forEach { putValueArgument(it, null) } } // restore to null
                callStack.popState()    // result of constructor must be dropped, because next instruction will be `IrGetEnumValue`
                callStack.dropSubFrame()
            }

            if (enumEntries.isNotEmpty() && enumSuperCall != null) {
                val valueArguments = listOf(
                    enumEntry.name.asString().toIrConst(irBuiltIns.stringType),
                    enumEntries.indexOf(enumEntry).toIrConst(irBuiltIns.intType)
                )
                valueArguments.forEachIndexed { index, irConst -> enumSuperCall.putValueArgument(index, irConst) }
            }

            val enumInitializer = enumEntry.initializerExpression?.expression
                ?: throw InterpreterError("Initializer at enum entry ${enumEntry.fqNameWhenAvailable} is null")
            val enumConstructorCall = enumInitializer as? IrEnumConstructorCall
                ?: (enumInitializer as IrBlock).statements.filterIsInstance<IrEnumConstructorCall>().single()

            val enumClassObject = Variable(enumConstructorCall.getThisReceiver(), Common(enumEntry.correspondingClass ?: enumClass))
            environment.mapOfEnums[enumEntry.symbol] = enumClassObject.state as Complex

            callStack.newSubFrame(enumEntry)
            callStack.addInstruction(CustomInstruction(cleanEnumSuperCall))
            callStack.addInstruction(CompoundInstruction(enumInitializer))
            callStack.addVariable(enumClassObject)
        }
    }

    private fun interpretTypeOperatorCall(expression: IrTypeOperatorCall) {
        val typeClassifier = expression.typeOperand.classifierOrFail
        val isReified = (typeClassifier.owner as? IrTypeParameter)?.isReified == true
        val isErased = typeClassifier.owner is IrTypeParameter && !isReified
        val typeOperand = expression.typeOperand.getTypeIfReified(callStack)

        val state = callStack.popState()
        when (expression.operator) {
            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                // do nothing
                // callStack.pushState(getUnitState()) TODO find real use cases for this
            }
            IrTypeOperator.CAST, IrTypeOperator.IMPLICIT_CAST -> {
                when {
                    // this first check is performed inside `isSubtypeOf` but repeated here to create separate exception
                    state.isNull() && !typeOperand.isNullable() -> NullPointerException().handleUserException(environment)
                    !isErased && !state.isSubtypeOf(typeOperand) -> {
                        val castedClassName = state.irClass.fqNameWhenAvailable
                        ClassCastException("$castedClassName cannot be cast to ${typeOperand.render()}").handleUserException(environment)
                    }
                    else -> callStack.pushState(state)
                }
            }
            IrTypeOperator.SAFE_CAST -> {
                when {
                    !isErased && !state.isSubtypeOf(typeOperand) -> callStack.pushState(null.toState(irBuiltIns.nothingNType))
                    else -> callStack.pushState(state)
                }
            }
            IrTypeOperator.INSTANCEOF -> {
                val isInstance = isErased || state.isSubtypeOf(typeOperand)
                callStack.pushState(isInstance.toState(irBuiltIns.booleanType))
            }
            IrTypeOperator.NOT_INSTANCEOF -> {
                val isInstance = isErased || state.isSubtypeOf(typeOperand)
                callStack.pushState((!isInstance).toState(irBuiltIns.booleanType))
            }
            IrTypeOperator.IMPLICIT_NOTNULL -> {
                when {
                    state.isNull() && !typeOperand.isNullable() -> NullPointerException().handleUserException(environment)
                    else -> callStack.pushState(state)
                }
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
                    else -> listOf(result.asProxy(callInterceptor))
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
        element.finallyExpression?.handleAndDropResult(callStack)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun interpretThrow(expression: IrThrow) {
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

        callStack.pushState(result.reversed().joinToString(separator = "").toState(expression.type))
    }

    private fun interpretFunctionExpression(expression: IrFunctionExpression) {
        val function = KFunctionState(expression.function, expression.type.classOrNull!!.owner)
        if (expression.function.isLocal) callStack.storeUpValues(function)
        callStack.pushState(function)
    }

    private fun interpretFunctionReference(reference: IrFunctionReference) {
        val irFunction = reference.symbol.owner

        val dispatchReceiver = reference.dispatchReceiver?.let { callStack.popState() }
        val extensionReceiver = reference.extensionReceiver?.let { callStack.popState() }

        val function = KFunctionState(
            reference,
            dispatchReceiver?.let { Variable(irFunction.getDispatchReceiver()!!, it) },
            extensionReceiver?.let { Variable(irFunction.getExtensionReceiver()!!, it) }
        )
        if (irFunction.isLocal) callStack.storeUpValues(function)
        callStack.pushState(function)
    }

    private fun interpretPropertyReference(propertyReference: IrPropertyReference) {
        // it is impossible to get KProperty2 through ::, so only one receiver can be not null (or both null)
        val receiver = (propertyReference.dispatchReceiver ?: propertyReference.extensionReceiver)?.let { callStack.popState() }
        val propertyState = KPropertyState(propertyReference, receiver)

        fun List<IrTypeParameter>.addToFields() {
            (0 until propertyReference.typeArgumentsCount).forEach { index ->
                val kTypeState = KTypeState(propertyReference.getTypeArgument(index)!!, irBuiltIns.anyClass.owner)
                propertyState.fields += Variable(this[index].symbol, kTypeState)
            }
        }
        propertyReference.getter?.owner?.typeParameters?.addToFields()
        propertyReference.setter?.owner?.typeParameters?.addToFields()

        callStack.pushState(propertyState)
    }

    private fun interpretClassReference(classReference: IrClassReference) {
        when (classReference.symbol) {
            is IrTypeParameterSymbol -> { // reified
                val kTypeState = callStack.getState(classReference.symbol) as KTypeState
                callStack.pushState(KClassState(kTypeState.irType.classOrNull!!.owner, classReference.type.classOrNull!!.owner))
            }
            else -> callStack.pushState(KClassState(classReference))
        }
    }

    private fun interpretGetClass(expression: IrGetClass) {
        val irClass = callStack.popState().irClass
        callStack.pushState(KClassState(irClass, expression.type.classOrNull!!.owner))
    }
}
