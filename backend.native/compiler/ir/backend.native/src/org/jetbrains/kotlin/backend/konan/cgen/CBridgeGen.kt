package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.common.ir.addFakeOverridesViaIncorrectHeuristic
import org.jetbrains.kotlin.backend.common.ir.createDispatchReceiverParameter
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.backend.konan.isObjCMetaClass
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrConstructorImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.backend.konan.getObjCMethodInfo
import org.jetbrains.kotlin.backend.konan.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.descriptors.*

internal interface KotlinStubs {
    val irBuiltIns: IrBuiltIns
    val symbols: KonanSymbols
    val target: KonanTarget
    fun addKotlin(declaration: IrDeclaration)
    fun addC(lines: List<String>)
    fun getUniqueCName(prefix: String): String
    fun getUniqueKotlinFunctionReferenceClassName(prefix: String): String

    fun reportError(location: IrElement, message: String): Nothing
    fun throwCompilerError(element: IrElement?, message: String): Nothing
}

private class KotlinToCCallBuilder(
        val irBuilder: IrBuilderWithScope,
        val stubs: KotlinStubs,
        val isObjCMethod: Boolean
) {

    val cBridgeName = stubs.getUniqueCName("knbridge")

    val symbols: KonanSymbols get() = stubs.symbols

    val bridgeCallBuilder = KotlinCallBuilder(irBuilder, symbols)
    val bridgeBuilder = KotlinCBridgeBuilder(irBuilder.startOffset, irBuilder.endOffset, cBridgeName, stubs, isKotlinToC = true)
    val cBridgeBodyLines = mutableListOf<String>()
    val cCallBuilder = CCallBuilder()
    val cFunctionBuilder = CFunctionBuilder()

}

private fun KotlinToCCallBuilder.passThroughBridge(argument: IrExpression, kotlinType: IrType, cType: CType): CVariable {
    bridgeCallBuilder.arguments += argument
    return bridgeBuilder.addParameter(kotlinType, cType).second
}

private fun KotlinToCCallBuilder.addArgument(
        argument: IrExpression,
        type: IrType,
        variadic: Boolean,
        parameter: IrValueParameter?
) {
    val argumentPassing = mapCalleeFunctionParameter(type, variadic, parameter, argument)
    addArgument(argument, argumentPassing, variadic)
}

private fun KotlinToCCallBuilder.addArgument(
        argument: IrExpression,
        argumentPassing: KotlinToCArgumentPassing,
        variadic: Boolean
) {
    val cArgument = with(argumentPassing) { passValue(argument) } ?: return
    cCallBuilder.arguments += cArgument.expression
    if (!variadic) cFunctionBuilder.addParameter(cArgument.type)
}

private fun KotlinToCCallBuilder.buildKotlinBridgeCall(transformCall: (IrMemberAccessExpression<*>) -> IrExpression = { it }): IrExpression =
        bridgeCallBuilder.build(
                bridgeBuilder.buildKotlinBridge().also {
                    this.stubs.addKotlin(it)
                },
                transformCall
        )

internal fun KotlinStubs.generateCCall(expression: IrCall, builder: IrBuilderWithScope, isInvoke: Boolean): IrExpression {
    require(expression.dispatchReceiver == null)

    val callBuilder = KotlinToCCallBuilder(builder, this, isObjCMethod = false)

    val callee = expression.symbol.owner as IrSimpleFunction

    // TODO: consider computing all arguments before converting.

    val targetPtrParameter: String?
    val targetFunctionName: String

    if (isInvoke) {
        targetPtrParameter = callBuilder.passThroughBridge(
                expression.extensionReceiver!!,
                symbols.interopCPointer.typeWithStarProjections,
                CTypes.voidPtr
        ).name
        targetFunctionName = "targetPtr"

        (0 until expression.valueArgumentsCount).forEach {
            callBuilder.addArgument(
                    expression.getValueArgument(it)!!,
                    type = expression.getTypeArgument(it)!!,
                    variadic = false,
                    parameter = null
            )
        }
    } else {
        require(expression.extensionReceiver == null)
        targetPtrParameter = null
        targetFunctionName = this.getUniqueCName("target")

        val arguments = (0 until expression.valueArgumentsCount).map {
            expression.getValueArgument(it)
        }
        callBuilder.addArguments(arguments, callee)
    }

    val returnValuePassing = if (isInvoke) {
        val returnType = expression.getTypeArgument(expression.typeArgumentsCount - 1)!!
        mapReturnType(returnType, TypeLocation.FunctionCallResult(expression), signature = null)
    } else {
        mapReturnType(callee.returnType, TypeLocation.FunctionCallResult(expression), signature = callee)
    }

    val result = callBuilder.buildCall(targetFunctionName, returnValuePassing)

    val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)

    if (isInvoke) {
        callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = ${targetPtrParameter!!};")
    } else {
        val cCallSymbolName = callee.getAnnotationArgumentValue<String>(cCall, "id")!!
        this.addC(listOf("extern const $targetFunctionVariable __asm(\"$cCallSymbolName\");")) // Exported from cinterop stubs.
    }

    callBuilder.emitCBridge()

    return result
}

private fun KotlinToCCallBuilder.addArguments(arguments: List<IrExpression?>, callee: IrFunction) {
    arguments.forEachIndexed { index, argument ->
        val parameter = callee.valueParameters[index]
        if (parameter.isVararg) {
            require(index == arguments.lastIndex)
            addVariadicArguments(argument)
            cFunctionBuilder.variadic = true
        } else {
            addArgument(argument!!, parameter.type, variadic = false, parameter = parameter)
        }
    }
}

private fun KotlinToCCallBuilder.addVariadicArguments(
        argumentForVarargParameter: IrExpression?
) = handleArgumentForVarargParameter(argumentForVarargParameter) { variable, elements ->
    if (variable == null) {
        unwrapVariadicArguments(elements).forEach {
            addArgument(it, it.type, variadic = true, parameter = null)
        }
    } else {
        // See comment in [handleArgumentForVarargParameter].
        // Array for this vararg parameter is already computed before the call,
        // so query statically known typed arguments from this array.

        with(irBuilder) {
            val argumentTypes = unwrapVariadicArguments(elements).map { it.type }
            argumentTypes.forEachIndexed { index, type ->
                val untypedArgument = irCall(symbols.arrayGet[symbols.array]!!.owner).apply {
                    dispatchReceiver = irGet(variable)
                    putValueArgument(0, irInt(index))
                }
                val argument = irAs(untypedArgument, type) // Note: this cast always succeeds.
                addArgument(argument, type, variadic = true, parameter = null)
            }
        }
    }
}

private fun KotlinToCCallBuilder.unwrapVariadicArguments(
        elements: List<IrVarargElement>
): List<IrExpression> = elements.flatMap {
    when (it) {
        is IrExpression -> listOf(it)
        is IrSpreadElement -> {
            val expression = it.expression
            if (expression is IrCall && expression.symbol == symbols.arrayOf) {
                handleArgumentForVarargParameter(expression.getValueArgument(0)) { _, elements ->
                    unwrapVariadicArguments(elements)
                }
            } else {
                stubs.reportError(it, "When calling variadic " +
                        (if (isObjCMethod) "Objective-C methods " else "C functions ") +
                                "spread operator is supported only for *arrayOf(...)")
            }
        }
        else -> stubs.throwCompilerError(it, "unexpected IrVarargElement")
    }
}

private fun <R> KotlinToCCallBuilder.handleArgumentForVarargParameter(
        argument: IrExpression?,
        block: (variable: IrVariable?, elements: List<IrVarargElement>) -> R
): R = when (argument) {

    null -> block(null, emptyList())

    is IrVararg -> block(null, argument.elements)

    is IrGetValue -> {
        /* This is possible when using named arguments with reordering, i.e.
         *
         *   foo(second = *arrayOf(...), first = ...)
         *
         * psi2ir generates as
         *
         *   val secondTmp = *arrayOf(...)
         *   val firstTmp = ...
         *   foo(firstTmp, secondTmp)
         *
         *
         **/

        val variable = argument.symbol.owner
        if (variable is IrVariable && variable.origin == IrDeclarationOrigin.IR_TEMPORARY_VARIABLE && !variable.isVar) {
            val initializer = variable.initializer
            if (initializer is IrVararg) {
                block(variable, initializer.elements)
            } else {
                stubs.throwCompilerError(initializer, "unexpected initializer")
            }
        } else if (variable is IrValueParameter && CallableReferenceLowering.isLoweredCallableReference(variable)) {
            val location = variable.parent // Parameter itself has incorrect location.
            val kind = if (this.isObjCMethod) "Objective-C methods" else "C functions"
            stubs.reportError(location, "callable references to variadic $kind are not supported")
        } else {
            stubs.throwCompilerError(variable, "unexpected value declaration")
        }
    }

    else -> stubs.throwCompilerError(argument, "unexpected vararg")
}

private fun KotlinToCCallBuilder.emitCBridge() {
    val cLines = mutableListOf<String>()

    cLines += "${bridgeBuilder.buildCSignature(cBridgeName)} {"
    cLines += cBridgeBodyLines
    cLines += "}"

    stubs.addC(cLines)
}

private fun KotlinToCCallBuilder.buildCall(
        targetFunctionName: String,
        returnValuePassing: ValueReturning
): IrExpression = with(returnValuePassing) {
    returnValue(cCallBuilder.build(targetFunctionName))
}

internal sealed class ObjCCallReceiver {
    class Regular(val rawPtr: IrExpression) : ObjCCallReceiver()
    class Retained(val rawPtr: IrExpression) : ObjCCallReceiver()
}

internal fun KotlinStubs.generateObjCCall(
        builder: IrBuilderWithScope,
        method: IrSimpleFunction,
        isStret: Boolean,
        selector: String,
        call: IrFunctionAccessExpression,
        superQualifier: IrClassSymbol?,
        receiver: ObjCCallReceiver,
        arguments: List<IrExpression?>
) = builder.irBlock {
    val callBuilder = KotlinToCCallBuilder(builder, this@generateObjCCall, isObjCMethod = true)

    val superClass = irTemporaryVar(
            superQualifier?.let { getObjCClass(symbols, it) } ?: irNullNativePtr(symbols)
    )

    val messenger = irCall(if (isStret) {
        symbols.interopGetMessengerStret
    } else {
        symbols.interopGetMessenger
    }.owner).apply {
        putValueArgument(0, irGet(superClass)) // TODO: check superClass statically.
    }

    val targetPtrParameter = callBuilder.passThroughBridge(
            messenger,
            symbols.interopCPointer.typeWithStarProjections,
            CTypes.voidPtr
    ).name
    val targetFunctionName = "targetPtr"

    val preparedReceiver = if (method.consumesReceiver()) {
        when (receiver) {
            is ObjCCallReceiver.Regular -> irCall(symbols.interopObjCRetain.owner).apply {
                putValueArgument(0, receiver.rawPtr)
            }

            is ObjCCallReceiver.Retained -> receiver.rawPtr
        }
    } else {
        when (receiver) {
            is ObjCCallReceiver.Regular -> receiver.rawPtr

            is ObjCCallReceiver.Retained -> {
                // Note: shall not happen: Retained is used only for alloc result currently,
                // which is used only as receiver for init methods, which are always receiver-consuming.
                // Can't even add a test for the code below.
                val rawPtrVar = scope.createTemporaryVariable(receiver.rawPtr)
                callBuilder.bridgeCallBuilder.prepare += rawPtrVar
                callBuilder.bridgeCallBuilder.cleanup += {
                    irCall(symbols.interopObjCRelease).apply {
                        putValueArgument(0, irGet(rawPtrVar)) // Balance retained pointer.
                    }
                }
                irGet(rawPtrVar)
            }
        }
    }

    val receiverOrSuper = if (superQualifier != null) {
        irCall(symbols.interopCreateObjCSuperStruct.owner).apply {
            putValueArgument(0, preparedReceiver)
            putValueArgument(1, irGet(superClass))
        }
    } else {
        preparedReceiver
    }

    callBuilder.cCallBuilder.arguments += callBuilder.passThroughBridge(
            receiverOrSuper, symbols.nativePtrType, CTypes.voidPtr).name
    callBuilder.cFunctionBuilder.addParameter(CTypes.voidPtr)

    callBuilder.cCallBuilder.arguments += "@selector($selector)"
    callBuilder.cFunctionBuilder.addParameter(CTypes.voidPtr)

    callBuilder.addArguments(arguments, method)

    val returnValuePassing =
            mapReturnType(method.returnType, TypeLocation.FunctionCallResult(call), signature = method)

    val result = callBuilder.buildCall(targetFunctionName, returnValuePassing)

    val targetFunctionVariable = CVariable(CTypes.pointer(callBuilder.cFunctionBuilder.getType()), targetFunctionName)
    callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = $targetPtrParameter;")

    callBuilder.emitCBridge()

    +result
}

private fun IrBuilderWithScope.getObjCClass(symbols: KonanSymbols, symbol: IrClassSymbol): IrExpression {
    val classDescriptor = symbol.descriptor
    assert(!classDescriptor.isObjCMetaClass())
    return irCall(symbols.interopGetObjCClass, symbols.nativePtrType, listOf(symbol.typeWithStarProjections))
}

private fun IrBuilderWithScope.irNullNativePtr(symbols: KonanSymbols) = irCall(symbols.getNativeNullPtr.owner)

private class CCallbackBuilder(
        val stubs: KotlinStubs,
        val location: IrElement,
        val isObjCMethod: Boolean
) {

    val irBuiltIns: IrBuiltIns get() = stubs.irBuiltIns
    val symbols: KonanSymbols get() = stubs.symbols

    private val cBridgeName = stubs.getUniqueCName("knbridge")

    fun buildCBridgeCall(): String = cBridgeCallBuilder.build(cBridgeName)
    fun buildCBridge(): String = bridgeBuilder.buildCSignature(cBridgeName)

    val bridgeBuilder = KotlinCBridgeBuilder(location.startOffset, location.endOffset, cBridgeName, stubs, isKotlinToC = false)
    val kotlinCallBuilder = KotlinCallBuilder(bridgeBuilder.kotlinIrBuilder, symbols)
    val kotlinBridgeStatements = mutableListOf<IrStatement>()
    val cBridgeCallBuilder = CCallBuilder()
    val cBodyLines = mutableListOf<String>()
    val cFunctionBuilder = CFunctionBuilder()

}

private fun CCallbackBuilder.passThroughBridge(
        cBridgeArgument: String,
        cBridgeParameterType: CType,
        kotlinBridgeParameterType: IrType
): IrValueParameter {
    cBridgeCallBuilder.arguments += cBridgeArgument
    return bridgeBuilder.addParameter(kotlinBridgeParameterType, cBridgeParameterType).first
}

private fun CCallbackBuilder.addParameter(it: IrValueParameter, functionParameter: IrValueParameter) {
    val typeLocation = if (isObjCMethod) {
        TypeLocation.ObjCMethodParameter(it.index, functionParameter)
    } else {
        TypeLocation.FunctionPointerParameter(cFunctionBuilder.numberOfParameters, location)
    }

    if (functionParameter.isVararg) {
        stubs.reportError(typeLocation.element, if (isObjCMethod) {
            "overriding variadic Objective-C methods is not supported"
        } else {
            "variadic function pointers are not supported"
        })
    }

    val valuePassing = stubs.mapFunctionParameterType(
            it.type,
            retained = it.isConsumed(),
            variadic = false,
            location = typeLocation
    )

    val kotlinArgument = with(valuePassing) { receiveValue() }
    kotlinCallBuilder.arguments += kotlinArgument
}

private fun CCallbackBuilder.build(function: IrSimpleFunction, signature: IrSimpleFunction): String {
    val typeLocation = if (isObjCMethod) {
        TypeLocation.ObjCMethodReturnValue(function)
    } else {
        TypeLocation.FunctionPointerReturnValue(location)
    }
    val valueReturning = stubs.mapReturnType(signature.returnType, typeLocation, signature)
    buildValueReturn(function, valueReturning)
    return buildCFunction()
}

private fun CCallbackBuilder.buildValueReturn(function: IrSimpleFunction, valueReturning: ValueReturning) {
    val kotlinCall = kotlinCallBuilder.build(function)
    with(valueReturning) {
        returnValue(kotlinCall)
    }

    val kotlinBridge = bridgeBuilder.buildKotlinBridge()
    kotlinBridge.body = bridgeBuilder.kotlinIrBuilder.irBlockBody {
        kotlinBridgeStatements.forEach { +it }
    }
    stubs.addKotlin(kotlinBridge)

    stubs.addC(listOf("${buildCBridge()};"))
}

private fun CCallbackBuilder.buildCFunction(): String {
    val result = stubs.getUniqueCName("kncfun")

    val cLines = mutableListOf<String>()

    cLines += "${cFunctionBuilder.buildSignature(result)} {"
    cLines += cBodyLines
    cLines += "}"

    stubs.addC(cLines)

    return result
}

private fun KotlinStubs.generateCFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
): String {
    val callbackBuilder = CCallbackBuilder(this, location, isObjCMethod)

    if (isObjCMethod) {
        val receiver = signature.dispatchReceiverParameter!!
        assert(isObjCReferenceType(receiver.type))
        val valuePassing = ObjCReferenceValuePassing(symbols, receiver.type, retained = signature.consumesReceiver())
        val kotlinArgument = with(valuePassing) { callbackBuilder.receiveValue() }
        callbackBuilder.kotlinCallBuilder.arguments += kotlinArgument

        // Selector is ignored:
        with(TrivialValuePassing(symbols.nativePtrType, CTypes.voidPtr)) { callbackBuilder.receiveValue() }
    } else {
        require(signature.dispatchReceiverParameter == null)
    }

    signature.extensionReceiverParameter?.let { callbackBuilder.addParameter(it, function.extensionReceiverParameter!!) }

    signature.valueParameters.forEach {
        callbackBuilder.addParameter(it, function.valueParameters[it.index])
    }

    return callbackBuilder.build(function, signature)
}

internal fun KotlinStubs.generateCFunctionPointer(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        expression: IrExpression
): IrExpression {
    val fakeFunction = generateCFunctionAndFakeKotlinExternalFunction(
            function,
            signature,
            isObjCMethod = false,
            location = expression
    )
    addKotlin(fakeFunction)

    return IrFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            fakeFunction.symbol,
            typeArgumentsCount = 0,
            reflectionTarget = null
    )
}

internal fun KotlinStubs.generateCFunctionAndFakeKotlinExternalFunction(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        location: IrElement
): IrSimpleFunction {
    val cFunction = generateCFunction(function, signature, isObjCMethod, location)
    return createFakeKotlinExternalFunction(signature, cFunction, isObjCMethod)
}

private fun KotlinStubs.createFakeKotlinExternalFunction(
        signature: IrSimpleFunction,
        cFunctionName: String,
        isObjCMethod: Boolean
): IrSimpleFunction {
    val bridgeDescriptor = WrappedSimpleFunctionDescriptor()
    val bridge = IrFunctionImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrSimpleFunctionSymbolImpl(bridgeDescriptor),
            Name.identifier(cFunctionName),
            Visibilities.PRIVATE,
            Modality.FINAL,
            signature.returnType,
            isInline = false,
            isExternal = true,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false,
            isInfix = false
    )
    bridgeDescriptor.bind(bridge)

    bridge.annotations += buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            symbols.symbolName.owner, cFunctionName)

    if (isObjCMethod) {
        val methodInfo = signature.getObjCMethodInfo()!!
        bridge.annotations += buildSimpleAnnotation(irBuiltIns, UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                symbols.objCMethodImp.owner, methodInfo.selector, methodInfo.encoding)
    }

    return bridge
}

private val cCall = RuntimeNames.cCall

private fun IrType.isUnsigned(unsignedType: UnsignedType) = this is IrSimpleType && !this.hasQuestionMark &&
        (this.classifier.owner as? IrClass)?.classId == unsignedType.classId

private fun IrType.isUByte() = this.isUnsigned(UnsignedType.UBYTE)
private fun IrType.isUShort() = this.isUnsigned(UnsignedType.USHORT)
private fun IrType.isUInt() = this.isUnsigned(UnsignedType.UINT)
private fun IrType.isULong() = this.isUnsigned(UnsignedType.ULONG)

internal fun IrType.isCEnumType(): Boolean {
    val simpleType = this as? IrSimpleType ?: return false
    if (simpleType.hasQuestionMark) return false
    val enumClass = simpleType.classifier.owner as? IrClass ?: return false
    if (!enumClass.isEnumClass) return false

    return enumClass.superTypes
            .any { (it.classifierOrNull?.owner as? IrClass)?.fqNameForIrSerialization == FqName("kotlinx.cinterop.CEnum") }
}

// Make sure external stubs always get proper annotaions.
private fun IrDeclaration.hasCCallAnnotation(name: String): Boolean =
        this.annotations.hasAnnotation(cCall.child(Name.identifier(name)))
                // LazyIr doesn't pass annotations from descriptor to IrValueParameter.
                || this.descriptor.annotations.hasAnnotation(cCall.child(Name.identifier(name)))


private fun IrValueParameter.isWCStringParameter() = hasCCallAnnotation("WCString")

private fun IrValueParameter.isCStringParameter() = hasCCallAnnotation("CString")

private fun IrValueParameter.isConsumed() = hasCCallAnnotation("Consumed")

private fun IrSimpleFunction.consumesReceiver() = hasCCallAnnotation("ConsumesReceiver")

private fun IrSimpleFunction.returnsRetained() = hasCCallAnnotation("ReturnsRetained")

private fun getStructSpelling(kotlinClass: IrClass): String? =
        kotlinClass.getAnnotationArgumentValue(FqName("kotlinx.cinterop.internal.CStruct"), "spelling")

private fun getCStructType(kotlinClass: IrClass): CType? =
        getStructSpelling(kotlinClass)?.let { CTypes.simple(it) }

private fun KotlinStubs.getNamedCStructType(kotlinClass: IrClass): CType? {
    val cStructType = getCStructType(kotlinClass) ?: return null
    val name = getUniqueCName("struct")
    addC(listOf("typedef ${cStructType.render(name)};"))
    return CTypes.simple(name)
}

// TODO: rework Boolean support.
// TODO: What should be used on watchOS?
private fun cBoolType(target: KonanTarget): CType? = when (target.family) {
    Family.IOS, Family.TVOS, Family.WATCHOS -> CTypes.C99Bool
    else -> CTypes.signedChar
}

private fun KotlinToCCallBuilder.mapCalleeFunctionParameter(
        type: IrType,
        variadic: Boolean,
        parameter: IrValueParameter?,
        argument: IrExpression
): KotlinToCArgumentPassing {
    val classifier = type.classifierOrNull
    return when {
        classifier == symbols.interopCValues || // Note: this should not be accepted, but is required for compatibility
                classifier == symbols.interopCValuesRef -> CValuesRefArgumentPassing

        classifier == symbols.string && (variadic || parameter?.isCStringParameter() == true) -> {
            if (variadic && isObjCMethod) {
                stubs.reportError(argument, "Passing String as variadic Objective-C argument is ambiguous; " +
                        "cast it to NSString or pass with '.cstr' as C string")
                // TODO: consider reporting a warning for C functions.
            }
            CStringArgumentPassing()
        }

        classifier == symbols.string && parameter?.isWCStringParameter() == true ->
            WCStringArgumentPassing()

        else -> stubs.mapFunctionParameterType(
                type,
                retained = parameter?.isConsumed() ?: false,
                variadic = variadic,
                location = TypeLocation.FunctionArgument(argument)
        )
    }
}

private fun KotlinStubs.mapFunctionParameterType(
        type: IrType,
        retained: Boolean,
        variadic: Boolean,
        location: TypeLocation
): ArgumentPassing = when {
    type.isUnit() && !variadic -> IgnoredUnitArgumentPassing
    else -> mapType(type, retained = retained, variadic = variadic, location = location)
}

private sealed class TypeLocation(val element: IrElement) {
    class FunctionArgument(val argument: IrExpression) : TypeLocation(argument)
    class FunctionCallResult(val call: IrFunctionAccessExpression) : TypeLocation(call)

    class FunctionPointerParameter(val index: Int, element: IrElement) : TypeLocation(element)
    class FunctionPointerReturnValue(element: IrElement) : TypeLocation(element)

    class ObjCMethodParameter(val index: Int, element: IrElement) : TypeLocation(element)
    class ObjCMethodReturnValue(element: IrElement) : TypeLocation(element)

    class BlockParameter(val index: Int, val blockLocation: TypeLocation) : TypeLocation(blockLocation.element)
    class BlockReturnValue(val blockLocation: TypeLocation) : TypeLocation(blockLocation.element)
}

private fun KotlinStubs.mapReturnType(
        type: IrType,
        location: TypeLocation,
        signature: IrSimpleFunction?
): ValueReturning = when {
    type.isUnit() -> VoidReturning
    else -> mapType(type, retained = signature?.returnsRetained() ?: false, variadic = false, location = location)
}

private fun KotlinStubs.mapBlockType(
        type: IrType,
        retained: Boolean,
        location: TypeLocation
): ObjCBlockPointerValuePassing {
    type as IrSimpleType
    require(type.classifier == symbols.functionN(type.arguments.size - 1))
    val returnTypeArgument = type.arguments.last()
    val valueReturning = when (returnTypeArgument) {
        is IrTypeProjection -> if (returnTypeArgument.variance == Variance.INVARIANT) {
            mapReturnType(returnTypeArgument.type, TypeLocation.BlockReturnValue(location), null)
        } else {
            reportUnsupportedType("${returnTypeArgument.variance.label}-variance of return type", type, location)
        }
        is IrStarProjection -> reportUnsupportedType("* as return type", type, location)
        else -> error(returnTypeArgument)
    }
    val parameterValuePassings = type.arguments.dropLast(1).mapIndexed { index, argument ->
        when (argument) {
            is IrTypeProjection -> if (argument.variance == Variance.INVARIANT) {
                mapType(
                        argument.type,
                        retained = false,
                        variadic = false,
                        location = TypeLocation.BlockParameter(index, location)
                )
            } else {
                reportUnsupportedType("${argument.variance.label}-variance of ${index + 1} parameter type", type, location)
            }
            is IrStarProjection -> reportUnsupportedType("* as ${index + 1} parameter type", type, location)
            else -> error(argument)
        }
    }
    return ObjCBlockPointerValuePassing(
            this,
            location.element,
            type,
            valueReturning,
            parameterValuePassings,
            retained
    )
}

private fun KotlinStubs.mapType(type: IrType, retained: Boolean, variadic: Boolean, location: TypeLocation): ValuePassing =
        mapType(type, retained, variadic, location, { reportUnsupportedType(it, type, location) })

private fun IrType.isTypeOfNullLiteral(): Boolean = this is IrSimpleType && hasQuestionMark
        && classifier.isClassWithFqName(KotlinBuiltIns.FQ_NAMES.nothing)

internal fun IrType.isVector(): Boolean {
    if (this is IrSimpleType && !this.hasQuestionMark) {
        return classifier.isClassWithFqName(KonanFqNames.Vector128.toUnsafe())
    }
    return false
}

private fun KotlinStubs.mapType(
        type: IrType,
        retained: Boolean,
        variadic: Boolean,
        typeLocation: TypeLocation,
        reportUnsupportedType: (String) -> Nothing
): ValuePassing = when {
    type.isBoolean() -> BooleanValuePassing(
            cBoolType(target) ?: reportUnsupportedType("unavailable on target platform"),
            irBuiltIns
    )

    type.isByte() -> TrivialValuePassing(irBuiltIns.byteType, CTypes.signedChar)
    type.isShort() -> TrivialValuePassing(irBuiltIns.shortType, CTypes.short)
    type.isInt() -> TrivialValuePassing(irBuiltIns.intType, CTypes.int)
    type.isLong() -> TrivialValuePassing(irBuiltIns.longType, CTypes.longLong)
    type.isFloat() -> TrivialValuePassing(irBuiltIns.floatType, CTypes.float)
    type.isDouble() -> TrivialValuePassing(irBuiltIns.doubleType, CTypes.double)
    type.classifierOrNull == symbols.interopCPointer -> TrivialValuePassing(type, CTypes.voidPtr)
    type.isTypeOfNullLiteral() && variadic  -> TrivialValuePassing(symbols.interopCPointer.typeWithStarProjections.makeNullable(), CTypes.voidPtr)
    type.isUByte() -> UnsignedValuePassing(type, CTypes.signedChar, CTypes.unsignedChar)
    type.isUShort() -> UnsignedValuePassing(type, CTypes.short, CTypes.unsignedShort)
    type.isUInt() -> UnsignedValuePassing(type, CTypes.int, CTypes.unsignedInt)
    type.isULong() -> UnsignedValuePassing(type, CTypes.longLong, CTypes.unsignedLongLong)

    type.isVector() -> TrivialValuePassing(type, CTypes.vector128)

    type.isCEnumType() -> {
        val enumClass = type.getClass()!!
        val value = enumClass.declarations
            .filterIsInstance<IrProperty>()
            .single { it.name.asString() == "value" }

        CEnumValuePassing(
                enumClass,
                value,
                mapType(value.getter!!.returnType, retained, variadic, typeLocation) as SimpleValuePassing
        )
    }

    type.classifierOrNull == symbols.interopCValue -> if (type.isNullable()) {
        reportUnsupportedType("must not be nullable")
    } else {
        val kotlinClass = (type as IrSimpleType).arguments.singleOrNull()?.typeOrNull?.getClass()
                ?: reportUnsupportedType("must be parameterized with concrete class")

        StructValuePassing(kotlinClass, getNamedCStructType(kotlinClass)
                ?: reportUnsupportedType("not a structure or too complex"))
    }

    type.classOrNull?.isSubtypeOfClass(symbols.nativePointed) == true -> {
        TrivialValuePassing(type, CTypes.voidPtr)
    }

    type.isFunction() -> if (variadic){
        reportUnsupportedType("not supported as variadic argument")
    } else {
        mapBlockType(type, retained = retained, location = typeLocation)
    }

    isObjCReferenceType(type) -> ObjCReferenceValuePassing(symbols, type, retained = retained)

    else -> reportUnsupportedType("doesn't correspond to any C type")
}

private fun KotlinStubs.isObjCReferenceType(type: IrType): Boolean {
    if (!target.family.isAppleFamily) return false

    // Handle the same types as produced by [objCPointerMirror] in Interop/StubGenerator/.../Mappings.kt.

    if (type.isObjCObjectType()) return true

    val descriptor = type.classifierOrNull?.descriptor ?: return false
    val builtIns = irBuiltIns.builtIns

    return when (descriptor) {
        builtIns.any,
        builtIns.string,
        builtIns.list, builtIns.mutableList,
        builtIns.set,
        builtIns.map -> true
        else -> false
    }
}

private class CExpression(val expression: String, val type: CType)

private interface KotlinToCArgumentPassing {
    fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression?
}

private interface ValueReturning {
    val cType: CType

    fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression
    fun CCallbackBuilder.returnValue(expression: IrExpression)
}

private interface ArgumentPassing : KotlinToCArgumentPassing {
    fun CCallbackBuilder.receiveValue(): IrExpression
}

private interface ValuePassing : ArgumentPassing, ValueReturning

private abstract class SimpleValuePassing : ValuePassing {
    abstract val kotlinBridgeType: IrType
    abstract val cBridgeType: CType
    override abstract val cType: CType
    open val callbackParameterCType get() = cType

    abstract fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression
    open fun IrBuilderWithScope.kotlinCallbackResultToBridged(expression: IrExpression): IrExpression =
            kotlinToBridged(expression)

    abstract fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression
    abstract fun bridgedToC(expression: String): String
    abstract fun cToBridged(expression: String): String

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val bridgeArgument = irBuilder.kotlinToBridged(expression)
        val cBridgeValue = passThroughBridge(bridgeArgument, kotlinBridgeType, cBridgeType).name
        return CExpression(bridgedToC(cBridgeValue), cType)
    }

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(kotlinBridgeType, cBridgeType)
        cBridgeBodyLines.add("return ${cToBridged(expression)};")
        val kotlinBridgeCall = buildKotlinBridgeCall()
        return irBuilder.bridgedToKotlin(kotlinBridgeCall, symbols)
    }

    override fun CCallbackBuilder.receiveValue(): IrExpression {
        val cParameter = cFunctionBuilder.addParameter(callbackParameterCType)
        val cBridgeArgument = cToBridged(cParameter.name)
        val kotlinParameter = passThroughBridge(cBridgeArgument, cBridgeType, kotlinBridgeType)
        return with(bridgeBuilder.kotlinIrBuilder) {
            bridgedToKotlin(irGet(kotlinParameter), symbols)
        }
    }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(kotlinBridgeType, cBridgeType)

        kotlinBridgeStatements += with(bridgeBuilder.kotlinIrBuilder) {
            irReturn(kotlinCallbackResultToBridged(expression))
        }
        val cBridgeCall = buildCBridgeCall()
        cBodyLines += "return ${bridgedToC(cBridgeCall)};"
    }
}

private class TrivialValuePassing(val kotlinType: IrType, override val cType: CType) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = kotlinType
    override val cBridgeType: CType
        get() = cType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression = expression
    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression = expression
    override fun bridgedToC(expression: String): String = expression
    override fun cToBridged(expression: String): String = expression
}

private class UnsignedValuePassing(val kotlinType: IrType, val cSignedType: CType, override val cType: CType) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = kotlinType
    override val cBridgeType: CType
        get() = cSignedType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression = expression

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression = expression

    override fun bridgedToC(expression: String): String = cType.cast(expression)

    override fun cToBridged(expression: String): String = cBridgeType.cast(expression)
}

private class BooleanValuePassing(override val cType: CType, private val irBuiltIns: IrBuiltIns) : SimpleValuePassing() {
    override val cBridgeType: CType get() = CTypes.signedChar
    override val kotlinBridgeType: IrType get() = irBuiltIns.byteType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression = irIfThenElse(
            irBuiltIns.byteType,
            condition = expression,
            thenPart = IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 1),
            elsePart = IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 0)
    )

    override fun IrBuilderWithScope.bridgedToKotlin(
            expression: IrExpression,
            symbols: KonanSymbols
    ): IrExpression = irNot(irCall(symbols.areEqualByValue[PrimitiveBinaryType.BYTE]!!.owner).apply {
        putValueArgument(0, expression)
        putValueArgument(1, IrConstImpl.byte(startOffset, endOffset, irBuiltIns.byteType, 0))
    })

    override fun bridgedToC(expression: String): String = cType.cast(expression)

    override fun cToBridged(expression: String): String = cBridgeType.cast(expression)
}

private class StructValuePassing(private val kotlinClass: IrClass, override val cType: CType) : ValuePassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cBridgeValue = passThroughBridge(
                cValuesRefToPointer(expression),
                symbols.interopCPointer.typeWithStarProjections,
                CTypes.pointer(cType)
        ).name

        return CExpression("*$cBridgeValue", cType)
    }

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression = with(irBuilder) {
        cFunctionBuilder.setReturnType(cType)
        bridgeBuilder.setReturnType(context.irBuiltIns.unitType, CTypes.void)

        val kotlinPointed = scope.createTemporaryVariable(irCall(symbols.interopAllocType.owner).apply {
            extensionReceiver = bridgeCallBuilder.getMemScope()
            putValueArgument(0, getTypeObject())
        })

        bridgeCallBuilder.prepare += kotlinPointed

        val cPointer = passThroughBridge(irGet(kotlinPointed), kotlinPointedType, CTypes.pointer(cType))
        cBridgeBodyLines += "*${cPointer.name} = $expression;"

        buildKotlinBridgeCall {
            irBlock {
                at(it)
                +it
                +readCValue(irGet(kotlinPointed), symbols)
            }
        }
    }

    override fun CCallbackBuilder.receiveValue(): IrExpression = with(bridgeBuilder.kotlinIrBuilder) {
        val cParameter = cFunctionBuilder.addParameter(cType)
        val kotlinPointed = passThroughBridge("&${cParameter.name}", CTypes.voidPtr, kotlinPointedType)

        readCValue(irGet(kotlinPointed), symbols)
    }

    private fun IrBuilderWithScope.readCValue(kotlinPointed: IrExpression, symbols: KonanSymbols): IrExpression =
        irCall(symbols.interopCValueRead.owner).apply {
            extensionReceiver = kotlinPointed
            putValueArgument(0, getTypeObject())
        }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) = with(bridgeBuilder.kotlinIrBuilder) {
        bridgeBuilder.setReturnType(irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(cType)

        val result = "callbackResult"
        val cReturnValue = CVariable(cType, result)
        cBodyLines += "$cReturnValue;"
        val kotlinPtr = passThroughBridge("&$result", CTypes.voidPtr, symbols.nativePtrType)

        kotlinBridgeStatements += irCall(symbols.interopCValueWrite.owner).apply {
            extensionReceiver = expression
            putValueArgument(0, irGet(kotlinPtr))
        }
        val cBridgeCall = buildCBridgeCall()
        cBodyLines += "$cBridgeCall;"
        cBodyLines += "return $result;"
    }

    private val kotlinPointedType: IrType get() = kotlinClass.defaultType

    private fun IrBuilderWithScope.getTypeObject() =
            irGetObject(
                    kotlinClass.declarations.filterIsInstance<IrClass>()
                            .single { it.isCompanion }.symbol
            )

}

private class CEnumValuePassing(
        val enumClass: IrClass,
        val value: IrProperty,
        val baseValuePassing: SimpleValuePassing
) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = baseValuePassing.kotlinBridgeType
    override val cBridgeType: CType
        get() = baseValuePassing.cBridgeType
    override val cType: CType
        get() = baseValuePassing.cType

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression {
        val value = irCall(value.getter!!).apply {
            dispatchReceiver = expression
        }

        return with(baseValuePassing) { kotlinToBridged(value) }
    }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression {
        val companionClass = enumClass.declarations.filterIsInstance<IrClass>().single { it.isCompanion }
        val byValue = companionClass.simpleFunctions().single { it.name.asString() == "byValue" }

        return irCall(byValue).apply {
            dispatchReceiver = irGetObject(companionClass.symbol)
            putValueArgument(0, expression)
        }
    }

    override fun bridgedToC(expression: String): String = with(baseValuePassing) { bridgedToC(expression) }
    override fun cToBridged(expression: String): String = with(baseValuePassing) { cToBridged(expression) }
}

private class ObjCReferenceValuePassing(
        private val symbols: KonanSymbols,
        private val type: IrType,
        private val retained: Boolean
) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = symbols.nativePtrType
    override val cBridgeType: CType
        get() = CTypes.voidPtr
    override val cType: CType
        get() = CTypes.voidPtr

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression {
        val ptr = irCall(symbols.interopObjCObjectRawValueGetter.owner).apply {
            extensionReceiver = expression
        }
        return if (retained) {
            irCall(symbols.interopObjCRetain).apply {
                putValueArgument(0, ptr)
            }
        } else {
            ptr
        }
    }

    override fun IrBuilderWithScope.kotlinCallbackResultToBridged(expression: IrExpression): IrExpression {
        if (retained) return kotlinToBridged(expression) // Optimization.
        // Kotlin code may loose the ownership on this pointer after returning from the bridge,
        // so retain the pointer and autorelease it:
        return irCall(symbols.interopObjcRetainAutoreleaseReturnValue.owner).apply {
            putValueArgument(0, kotlinToBridged(expression))
        }
        // TODO: optimize by using specialized Kotlin-to-ObjC converter.
    }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            convertPossiblyRetainedObjCPointer(symbols, retained, expression) {
                irCall(symbols.interopInterpretObjCPointerOrNull, listOf(type)).apply {
                    putValueArgument(0, it)
                }
            }

    override fun bridgedToC(expression: String): String = expression
    override fun cToBridged(expression: String): String = expression

}

private fun IrBuilderWithScope.convertPossiblyRetainedObjCPointer(
        symbols: KonanSymbols,
        retained: Boolean,
        pointer: IrExpression,
        convert: (IrExpression) -> IrExpression
): IrExpression = if (retained) {
    irBlock(startOffset, endOffset) {
        val ptrVar = irTemporary(pointer)
        val resultVar = irTemporary(convert(irGet(ptrVar)))
        +irCall(symbols.interopObjCRelease.owner).apply {
            putValueArgument(0, irGet(ptrVar))
        }
        +irGet(resultVar)
    }
} else {
    convert(pointer)
}

private class ObjCBlockPointerValuePassing(
        val stubs: KotlinStubs,
        private val location: IrElement,
        private val functionType: IrSimpleType,
        private val valueReturning: ValueReturning,
        private val parameterValuePassings: List<ValuePassing>,
        private val retained: Boolean
) : SimpleValuePassing() {
    val symbols get() = stubs.symbols

    override val kotlinBridgeType: IrType
        get() = symbols.nativePtrType
    override val cBridgeType: CType
        get() = CTypes.id
    override val cType: CType
        get() = CTypes.id

    /**
     * Callback can receive stack-allocated block. Using block type for parameter and passing it as `id` to the bridge
     * makes Objective-C compiler generate proper copying to heap.
     */
    override val callbackParameterCType: CType
        get() = CTypes.blockPointer(
                CTypes.function(
                        valueReturning.cType,
                        parameterValuePassings.map { it.cType },
                        variadic = false
                ))

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression =
            irCall(symbols.interopCreateKotlinObjectHolder.owner).apply {
                putValueArgument(0, expression)
            }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            irLetS(expression) { blockPointerVarSymbol ->
                val blockPointerVar = blockPointerVarSymbol.owner
                irIfThenElse(
                        functionType.makeNullable(),
                        condition = irCall(symbols.areEqualByValue.getValue(PrimitiveBinaryType.POINTER).owner).apply {
                            putValueArgument(0, irGet(blockPointerVar))
                            putValueArgument(1, irNullNativePtr(symbols))
                        },
                        thenPart = irNull(),
                        elsePart = convertPossiblyRetainedObjCPointer(symbols, retained, irGet(blockPointerVar)) {
                            createKotlinFunctionObject(it)
                        }
                )
            }

    private object OBJC_BLOCK_FUNCTION_IMPL : IrDeclarationOriginImpl("OBJC_BLOCK_FUNCTION_IMPL")

    private fun IrBuilderWithScope.createKotlinFunctionObject(blockPointer: IrExpression): IrExpression {
        val constructor = generateKotlinFunctionClass()
        return irCall(constructor).apply {
            putValueArgument(0, blockPointer)
        }
    }

    private fun IrBuilderWithScope.generateKotlinFunctionClass(): IrConstructor {
        val symbols = stubs.symbols

        val classDescriptor = WrappedClassDescriptor()
        val irClass = IrClassImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL, IrClassSymbolImpl(classDescriptor),
                Name.identifier(stubs.getUniqueKotlinFunctionReferenceClassName("BlockFunctionImpl")),
                ClassKind.CLASS, Visibilities.PRIVATE, Modality.FINAL,
                isCompanion = false, isInner = false, isData = false, isExternal = false,
                isInline = false, isExpect = false, isFun = false
        )
        classDescriptor.bind(irClass)
        irClass.createParameterDeclarations()

        irClass.superTypes += stubs.irBuiltIns.anyType
        irClass.superTypes += functionType.makeNotNull()

        val blockHolderField = createField(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                stubs.irBuiltIns.anyType,
                Name.identifier("blockHolder"),
                isMutable = false, owner = irClass
        )

        val constructorDescriptor = WrappedClassConstructorDescriptor()
        val constructor = IrConstructorImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                IrConstructorSymbolImpl(constructorDescriptor),
                Name.special("<init>"),
                Visibilities.PUBLIC,
                irClass.defaultType,
                isInline = false, isExternal = false, isPrimary = true, isExpect = false
        )
        constructorDescriptor.bind(constructor)
        irClass.addChild(constructor)

        val constructorParameterDescriptor = WrappedValueParameterDescriptor()
        val constructorParameter = IrValueParameterImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                IrValueParameterSymbolImpl(constructorParameterDescriptor),
                Name.identifier("blockPointer"),
                0,
                symbols.nativePtrType,
                varargElementType = null, isCrossinline = false, isNoinline = false
        )
        constructorParameterDescriptor.bind(constructorParameter)
        constructor.valueParameters += constructorParameter
        constructorParameter.parent = constructor

        constructor.body = irBuilder(stubs.irBuiltIns, constructor.symbol).irBlockBody(startOffset, endOffset) {
            +irDelegatingConstructorCall(symbols.any.owner.constructors.single())
            +irSetField(irGet(irClass.thisReceiver!!), blockHolderField,
                    irCall(symbols.interopCreateObjCObjectHolder.owner).apply {
                        putValueArgument(0, irGet(constructorParameter))
                    })
        }

        val parameterCount = parameterValuePassings.size
        assert(functionType.arguments.size == parameterCount + 1)

        val overriddenInvokeMethod = (functionType.classifier.owner as IrClass).simpleFunctions()
                .single { it.name == OperatorNameConventions.INVOKE }

        val invokeMethodDescriptor = WrappedSimpleFunctionDescriptor()
        val invokeMethod = IrFunctionImpl(
                startOffset, endOffset,
                OBJC_BLOCK_FUNCTION_IMPL,
                IrSimpleFunctionSymbolImpl(invokeMethodDescriptor),
                overriddenInvokeMethod.name,
                Visibilities.PUBLIC, Modality.FINAL,
                returnType = functionType.arguments.last().typeOrNull!!,
                isInline = false, isExternal = false, isTailrec = false, isSuspend = false, isExpect = false,
                isFakeOverride = false, isOperator = false, isInfix = false
        )
        invokeMethodDescriptor.bind(invokeMethod)
        invokeMethod.overriddenSymbols += overriddenInvokeMethod.symbol
        irClass.addChild(invokeMethod)
        invokeMethod.createDispatchReceiverParameter()

        invokeMethod.valueParameters += (0 until parameterCount).map { index ->
            val parameterDescriptor = WrappedValueParameterDescriptor()
            val parameter = IrValueParameterImpl(
                    startOffset, endOffset,
                    OBJC_BLOCK_FUNCTION_IMPL,
                    IrValueParameterSymbolImpl(parameterDescriptor),
                    Name.identifier("p$index"),
                    index,
                    functionType.arguments[index].typeOrNull!!,
                    varargElementType = null, isCrossinline = false, isNoinline = false
            )
            parameterDescriptor.bind(parameter)
            parameter.parent = invokeMethod
            parameter
        }

        invokeMethod.body = irBuilder(stubs.irBuiltIns, invokeMethod.symbol).irBlockBody(startOffset, endOffset) {
            val blockPointer = irCall(symbols.interopObjCObjectRawValueGetter.owner).apply {
                extensionReceiver = irGetField(irGet(invokeMethod.dispatchReceiverParameter!!), blockHolderField)
            }

            val arguments = (0 until parameterCount).map { index ->
                irGet(invokeMethod.valueParameters[index])
            }

            +irReturn(callBlock(blockPointer, arguments))
        }

        irClass.addFakeOverridesViaIncorrectHeuristic()

        stubs.addKotlin(irClass)
        return constructor
    }

    private fun IrBuilderWithScope.callBlock(blockPtr: IrExpression, arguments: List<IrExpression>): IrExpression {
        val callBuilder = KotlinToCCallBuilder(this, stubs, isObjCMethod = false)

        val rawBlockPointerParameter =  callBuilder.passThroughBridge(blockPtr, blockPtr.type, CTypes.id)
        val blockVariableName = "block"

        arguments.forEachIndexed { index, argument ->
            callBuilder.addArgument(argument, parameterValuePassings[index], variadic = false)
        }

        val result = callBuilder.buildCall(blockVariableName, valueReturning)

        val blockVariableType = CTypes.blockPointer(callBuilder.cFunctionBuilder.getType())
        val blockVariable = CVariable(blockVariableType, blockVariableName)
        callBuilder.cBridgeBodyLines.add(0, "$blockVariable = ${rawBlockPointerParameter.name};")

        callBuilder.emitCBridge()

        return result
    }

    override fun bridgedToC(expression: String): String {
        val callbackBuilder = CCallbackBuilder(stubs, location, isObjCMethod = false)
        val kotlinFunctionHolder = "kotlinFunctionHolder"

        callbackBuilder.cBridgeCallBuilder.arguments += kotlinFunctionHolder
        val (kotlinFunctionHolderParameter, _) =
                callbackBuilder.bridgeBuilder.addParameter(symbols.nativePtrType, CTypes.id)

        callbackBuilder.kotlinCallBuilder.arguments += with(callbackBuilder.bridgeBuilder.kotlinIrBuilder) {
            // TODO: consider casting to [functionType].
            irCall(symbols.interopUnwrapKotlinObjectHolderImpl.owner).apply {
                putValueArgument(0, irGet(kotlinFunctionHolderParameter) )
            }
        }

        parameterValuePassings.forEach {
            callbackBuilder.kotlinCallBuilder.arguments += with(it) {
                callbackBuilder.receiveValue()
            }
        }

        assert(functionType.isFunction())
        val invokeFunction = (functionType.classifier.owner as IrClass)
                .simpleFunctions().single { it.name == OperatorNameConventions.INVOKE }

        callbackBuilder.buildValueReturn(invokeFunction, valueReturning)

        val block = buildString {
            append('^')
            append(callbackBuilder.cFunctionBuilder.buildSignature(""))
            append(" { ")
            callbackBuilder.cBodyLines.forEach {
                append(it)
                append(' ')
            }
            append(" }")
        }
        val blockAsId = if (retained) {
            "(__bridge id)(__bridge_retained void*)$block" // Retain and convert to id.
        } else {
            "(id)$block"
        }

        return "({ id $kotlinFunctionHolder = $expression; $kotlinFunctionHolder ? $blockAsId : (id)0; })"
    }

    override fun cToBridged(expression: String) = expression

}

private class WCStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val wcstr = irBuilder.irSafeTransform(expression) {
            irCall(symbols.interopWcstr.owner).apply {
                extensionReceiver = it
            }
        }
        return with(CValuesRefArgumentPassing) { passValue(wcstr) }
    }

}

private class CStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cstr = irBuilder.irSafeTransform(expression) {
            irCall(symbols.interopCstr.owner).apply {
                extensionReceiver = it
            }
        }
        return with(CValuesRefArgumentPassing) { passValue(cstr) }
    }

}

private object CValuesRefArgumentPassing : KotlinToCArgumentPassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val bridgeArgument = cValuesRefToPointer(expression)
        val cBridgeValue = passThroughBridge(
                bridgeArgument,
                symbols.interopCPointer.typeWithStarProjections.makeNullable(),
                CTypes.voidPtr
        )
        return CExpression(cBridgeValue.name, cBridgeValue.type)
    }
}

private fun KotlinToCCallBuilder.cValuesRefToPointer(
        value: IrExpression
): IrExpression = if (value.type.classifierOrNull == symbols.interopCPointer) {
    value // Optimization
} else {
    val getPointerFunction = symbols.interopCValuesRef.owner
            .simpleFunctions()
            .single { it.name.asString() == "getPointer" }

    irBuilder.irSafeTransform(value) {
        irCall(getPointerFunction).apply {
            dispatchReceiver = it
            putValueArgument(0, bridgeCallBuilder.getMemScope())
        }
    }
}

private fun IrBuilderWithScope.irSafeTransform(
        value: IrExpression,
        block: IrBuilderWithScope.(IrExpression) -> IrExpression
): IrExpression = if (!value.type.isNullable()) {
    block(value) // Optimization
} else {
    irLetS(value) { valueVarSymbol ->
        val valueVar = valueVarSymbol.owner
        val transformed = block(irGet(valueVar))
        irIfThenElse(
                type = transformed.type.makeNullable(),
                condition = irEqeqeq(irGet(valueVar), irNull()),
                thenPart = irNull(),
                elsePart = transformed
        )
    }
}

private object VoidReturning : ValueReturning {
    override val cType: CType
        get() = CTypes.void

    override fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression {
        bridgeBuilder.setReturnType(irBuilder.context.irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(CTypes.void)
        cBridgeBodyLines += "$expression;"
        return buildKotlinBridgeCall()
    }

    override fun CCallbackBuilder.returnValue(expression: IrExpression) {
        bridgeBuilder.setReturnType(irBuiltIns.unitType, CTypes.void)
        cFunctionBuilder.setReturnType(CTypes.void)
        kotlinBridgeStatements += bridgeBuilder.kotlinIrBuilder.irReturn(expression)
        cBodyLines += "${buildCBridgeCall()};"
    }
}

private object IgnoredUnitArgumentPassing : ArgumentPassing {
    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression? {
        // Note: it is not correct to just drop the expression (due to possible side effects),
        // so (in lack of other options) evaluate the expression and pass ignored value to the bridge:
        val bridgeArgument = irBuilder.irBlock {
            +expression
            +irInt(0)
        }
        passThroughBridge(bridgeArgument, irBuilder.context.irBuiltIns.intType, CTypes.int).name
        return null
    }

    override fun CCallbackBuilder.receiveValue(): IrExpression {
        return bridgeBuilder.kotlinIrBuilder.irGetObject(irBuiltIns.unitClass)
    }
}

internal fun CType.cast(expression: String): String = "((${this.render("")})$expression)"

private fun KotlinStubs.reportUnsupportedType(reason: String, type: IrType, location: TypeLocation): Nothing {
    // TODO: report errors in frontend instead.
    fun TypeLocation.render(): String = when (this) {
        is TypeLocation.FunctionArgument -> ""
        is TypeLocation.FunctionCallResult -> " of return value"
        is TypeLocation.FunctionPointerParameter -> " of callback parameter ${index + 1}"
        is TypeLocation.FunctionPointerReturnValue -> " of callback return value"
        is TypeLocation.ObjCMethodParameter -> " of overridden Objective-C method parameter"
        is TypeLocation.ObjCMethodReturnValue -> " of overridden Objective-C method return value"
        is TypeLocation.BlockParameter -> " of ${index + 1} parameter in Objective-C block type${blockLocation.render()}"
        is TypeLocation.BlockReturnValue -> " of return value of Objective-C block type${blockLocation.render()}"
    }

    val typeLocation: String = location.render()

    reportError(location.element, "type ${type.render()} $typeLocation is not supported here" +
            if (reason.isNotEmpty()) ": $reason" else "")
}
