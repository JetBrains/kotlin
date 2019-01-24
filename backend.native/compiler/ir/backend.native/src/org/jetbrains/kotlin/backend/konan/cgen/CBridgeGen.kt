package org.jetbrains.kotlin.backend.konan.cgen

import org.jetbrains.kotlin.backend.common.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.backend.common.lower.at
import org.jetbrains.kotlin.backend.common.lower.irNot
import org.jetbrains.kotlin.backend.konan.PrimitiveBinaryType
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.createAnnotation
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.irasdescriptors.*
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrSpreadElement
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal interface KotlinStubs {
    val irBuiltIns: IrBuiltIns
    val symbols: KonanSymbols
    val target: KonanTarget
    fun addKotlin(declaration: IrDeclaration)
    fun addC(lines: List<String>)
    fun getUniqueCName(prefix: String): String

    fun reportError(location: IrElement, message: String): Nothing
}

private class KotlinToCCallBuilder(
        val irBuilder: IrBuilderWithScope,
        cBridgeName: String,
        val stubs: KotlinStubs
) {

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
    val argumentPassing = mapParameter(type, variadic, parameter, argument)
    val cArgument = with(argumentPassing) { passValue(argument) }
    cCallBuilder.arguments += cArgument.expression
    if (!variadic) cFunctionBuilder.addParameter(cArgument.type)
}

private fun KotlinToCCallBuilder.buildKotlinBridgeCall(transformCall: (IrCall) -> IrExpression = { it }): IrExpression =
        bridgeCallBuilder.build(
                bridgeBuilder.buildKotlinBridge().also {
                    this.stubs.addKotlin(it)
                },
                transformCall
        )

internal fun KotlinStubs.generateCCall(expression: IrCall, builder: IrBuilderWithScope, isInvoke: Boolean): IrExpression {
    require(expression.dispatchReceiver == null)

    val cBridgeName = this.getUniqueCName("knbridge")
    val callBuilder = KotlinToCCallBuilder(builder, cBridgeName, this)

    val cFunctionBuilder = callBuilder.cFunctionBuilder

    val callee = expression.symbol.owner

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

        (0 until expression.valueArgumentsCount).forEach { index ->
            val parameter = callee.valueParameters[index]
            val argument = expression.getValueArgument(index)
            if (parameter.isVararg) {
                require(index == expression.valueArgumentsCount - 1)
                require(argument is IrVararg?)
                argument?.elements.orEmpty().forEach {
                    when (it) {
                        is IrExpression -> callBuilder.addArgument(it, it.type, variadic = true, parameter = null)

                        is IrSpreadElement ->
                            reportError(it, "spread operator is not supported for variadic C functions")

                        else -> error(it)
                    }
                }
                cFunctionBuilder.variadic = true
            } else {
                callBuilder.addArgument(argument!!, parameter.type, variadic = false, parameter = parameter)
            }
        }
    }

    val cLines = mutableListOf<String>()

    val returnValuePassing = if (isInvoke) {
        val returnType = expression.getTypeArgument(expression.typeArgumentsCount - 1)!!
        mapReturnType(returnType, TypeLocation.FunctionCallResult(expression))
    } else {
        mapReturnType(callee.returnType, TypeLocation.FunctionCallResult(expression))
    }

    val result = with(returnValuePassing) {
        callBuilder.returnValue(callBuilder.cCallBuilder.build(targetFunctionName))
    }

    val targetFunctionVariable = CVariable(CTypes.pointer(cFunctionBuilder.getType()), targetFunctionName)
    if (isInvoke) {
        callBuilder.cBridgeBodyLines.add(0, "$targetFunctionVariable = ${targetPtrParameter!!};")
    } else {
        val cCallSymbolName = callee.getAnnotationArgumentValue<String>(cCall, "id")!!
        cLines += "extern const $targetFunctionVariable __asm(\"$cCallSymbolName\");" // Exported from cinterop stubs.
    }

    cLines += "${callBuilder.bridgeBuilder.buildCSignature(cBridgeName)} {"
    cLines += callBuilder.cBridgeBodyLines
    cLines += "}"

    this.addC(cLines)

    return result
}

private class CCallbackBuilder(
        val stubs: KotlinStubs,
        val location: IrElement
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

private fun CCallbackBuilder.addParameter(it: IrValueParameter) {
    val typeLocation = TypeLocation.FunctionPointerParameter(cFunctionBuilder.numberOfParameters, location)
    val valuePassing = stubs.mapType(it.type, typeLocation)

    val kotlinArgument = with(valuePassing) { receiveValue() }
    kotlinCallBuilder.arguments += kotlinArgument
}

private fun CCallbackBuilder.build(function: IrSimpleFunction, signature: IrFunction): String {
    val kotlinCall = kotlinCallBuilder.build(function)

    with(stubs.mapReturnType(signature.returnType, TypeLocation.FunctionPointerReturnValue(location))) {
        returnValue(kotlinCall)
    }

    val cLines = mutableListOf<String>()

    val kotlinBridge = bridgeBuilder.buildKotlinBridge()
    kotlinBridge.body = bridgeBuilder.kotlinIrBuilder.irBlockBody {
        kotlinBridgeStatements.forEach { +it }
    }
    stubs.addKotlin(kotlinBridge)

    val result = stubs.getUniqueCName("kncfun")

    cLines += "${buildCBridge()};"
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
    val callbackBuilder = CCallbackBuilder(this, location)

    signature.dispatchReceiverParameter?.let { callbackBuilder.addParameter(it) }
    if (isObjCMethod) {
//        assert(mapType(signature.dispatchReceiverParameter!!.type) is ObjCReferenceValuePassing)
        // Selector is ignored:
        with(TrivialValuePassing(symbols.nativePtrType, CTypes.voidPtr)) { callbackBuilder.receiveValue() }
    }
    signature.extensionReceiverParameter?.let { callbackBuilder.addParameter(it) }

    signature.valueParameters.forEach {
        callbackBuilder.addParameter(it)
    }

    return callbackBuilder.build(function, signature)
}

internal fun KotlinStubs.generateCFunctionPointer(
        function: IrSimpleFunction,
        signature: IrSimpleFunction,
        isObjCMethod: Boolean,
        expression: IrExpression
): IrExpression {
    val cFunction = generateCFunction(function, signature, isObjCMethod, expression)
    val fakeFunction = createFakeKotlinExternalFunction(signature, cFunction, isObjCMethod)
    addKotlin(fakeFunction)

    return IrFunctionReferenceImpl(
            expression.startOffset,
            expression.endOffset,
            expression.type,
            fakeFunction.symbol,
            fakeFunction.descriptor,
            0
    )
}

private fun KotlinStubs.createFakeKotlinExternalFunction(
        signature: IrSimpleFunction,
        cFunctionName: String,
        isObjCMethod: Boolean
): IrSimpleFunction {
    val objCMethodImpAnnotation = if (isObjCMethod) {
        val methodInfo = signature.getObjCMethodInfo()!!
        createObjCMethodImpAnnotation(methodInfo.selector, methodInfo.encoding, symbols)
    } else {
        null
    }
    val bridgeAnnotations = Annotations.create(
            listOfNotNull(
                    createAnnotation(symbols.symbolName.descriptor, "value" to cFunctionName),
                    objCMethodImpAnnotation
            )
    )
    val bridgeDescriptor = WrappedSimpleFunctionDescriptor(bridgeAnnotations)
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
            isSuspend = false
    )
    bridgeDescriptor.bind(bridge)

    return bridge
}

private fun createObjCMethodImpAnnotation(selector: String, encoding: String, symbols: KonanSymbols) =
        createAnnotation(symbols.objCMethodImp.descriptor, "selector" to selector, "encoding" to encoding)

private val cCall = RuntimeNames.cCall

private fun IrType.isUnsigned(unsignedType: UnsignedType) = this is IrSimpleType && !this.hasQuestionMark &&
        (this.classifier.owner as? IrClass)?.classId == unsignedType.classId

private fun IrType.isUByte() = this.isUnsigned(UnsignedType.UBYTE)
private fun IrType.isUShort() = this.isUnsigned(UnsignedType.USHORT)
private fun IrType.isUInt() = this.isUnsigned(UnsignedType.UINT)
private fun IrType.isULong() = this.isUnsigned(UnsignedType.ULONG)

private fun IrType.isCEnumType(): Boolean {
    val simpleType = this as? IrSimpleType ?: return false
    if (simpleType.hasQuestionMark) return false
    val enumClass = simpleType.classifier.owner as? IrClass ?: return false
    if (!enumClass.isEnumClass) return false

    return enumClass.superTypes
            .any { (it.classifierOrNull?.owner as? IrClass)?.fqNameSafe == FqName("kotlinx.cinterop.CEnum") }
}

// TODO: get rid of consulting descriptors for annotations.
// Make sure external stubs always get proper annotaions.
private fun IrValueParameter.isWCStringParameter() =
        this.annotations.hasAnnotation(cCall.child(Name.identifier("WCString"))) ||
        this.descriptor.annotations.hasAnnotation(cCall.child(Name.identifier("WCString")))

private fun IrValueParameter.isCStringParameter() =
        this.annotations.hasAnnotation(cCall.child(Name.identifier("CString"))) ||
        this.descriptor.annotations.hasAnnotation(cCall.child(Name.identifier("CString")))

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
private fun cBoolType(target: KonanTarget): CType? = when (target.family) {
    Family.IOS -> CTypes.C99Bool
    else -> CTypes.signedChar
}

private fun KotlinToCCallBuilder.mapParameter(
        type: IrType,
        variadic: Boolean,
        parameter: IrValueParameter?,
        argument: IrExpression
): KotlinToCArgumentPassing {
    val classifier = type.classifierOrNull
    return when {
        classifier == symbols.interopCValues || // Note: this should not be accepted, but is required for compatibility
                classifier == symbols.interopCValuesRef -> CValuesRefArgumentPassing

        classifier == symbols.string && (variadic || parameter?.isCStringParameter() == true) ->
            CStringArgumentPassing()

        classifier == symbols.string && parameter?.isWCStringParameter() == true ->
            WCStringArgumentPassing()

        else -> stubs.mapType(type, TypeLocation.FunctionArgument(argument))
    }
}

private sealed class TypeLocation(val element: IrElement) {
    class FunctionArgument(val argument: IrExpression) : TypeLocation(argument)
    class FunctionCallResult(val call: IrCall) : TypeLocation(call)

    class FunctionPointerParameter(val index: Int, element: IrElement) : TypeLocation(element)
    class FunctionPointerReturnValue(element: IrElement) : TypeLocation(element)
}

private fun KotlinStubs.mapReturnType(type: IrType, location: TypeLocation): ValueReturning =
        when {
            type.isUnit() -> VoidReturning
            else -> mapType(type, location)
        }

private fun KotlinStubs.mapType(type: IrType, location: TypeLocation): ValuePassing =
        mapType(type, { reportUnsupportedType(it, type, location) })

private fun KotlinStubs.mapType(type: IrType, reportUnsupportedType: (String) -> Nothing): ValuePassing = when {
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
    type.isUByte() -> UnsignedValuePassing(type, CTypes.signedChar, CTypes.unsignedChar)
    type.isUShort() -> UnsignedValuePassing(type, CTypes.short, CTypes.unsignedShort)
    type.isUInt() -> UnsignedValuePassing(type, CTypes.int, CTypes.unsignedInt)
    type.isULong() -> UnsignedValuePassing(type, CTypes.longLong, CTypes.unsignedLongLong)

    type.isCEnumType() -> {
        val enumClass = type.getClass()!!
        val value = enumClass.declarations
            .filterIsInstance<IrProperty>()
            .single { it.name.asString() == "value" }

        CEnumValuePassing(enumClass, value, mapType(value.getter!!.returnType, reportUnsupportedType) as SimpleValuePassing)
    }

    type.classifierOrNull == symbols.interopCValue -> if (type.isNullable()) {
        reportUnsupportedType("must not be nullable")
    } else {
        val kotlinClass = (type as IrSimpleType).arguments.singleOrNull()?.typeOrNull?.getClass()
                ?: reportUnsupportedType("must be parameterized with concrete class")

        StructValuePassing(kotlinClass, getNamedCStructType(kotlinClass)
                ?: reportUnsupportedType("not a structure or too complex"))
    }

    type.isFunction() -> reportUnsupportedType("")
    isObjCReferenceType(type) -> ObjCReferenceValuePassing(symbols, type)

    else -> reportUnsupportedType("doesn't correspond to any C type")
}

private fun KotlinStubs.isObjCReferenceType(type: IrType): Boolean {
    if (target.family != Family.OSX && target.family != Family.IOS) return false

    if (type.isObjCObjectType()) return true

    val descriptor = type.classifierOrNull?.descriptor ?: return false
    val builtIns = irBuiltIns.builtIns

    return when (descriptor) {
        builtIns.string,
        builtIns.list, builtIns.mutableList,
        builtIns.set, builtIns.mutableSet,
        builtIns.map, builtIns.mutableMap -> true
        else -> false
    }
}

private class CExpression(val expression: String, val type: CType)

private interface KotlinToCArgumentPassing {
    fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression
}

private interface ValueReturning {
    fun KotlinToCCallBuilder.returnValue(expression: String): IrExpression
    fun CCallbackBuilder.returnValue(expression: IrExpression)
}

private interface ValuePassing : KotlinToCArgumentPassing, ValueReturning {
    fun CCallbackBuilder.receiveValue(): IrExpression
}

private abstract class SimpleValuePassing : ValuePassing {
    abstract val kotlinBridgeType: IrType
    abstract val cBridgeType: CType
    abstract val cType: CType
    abstract fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression
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
        val cParameter = cFunctionBuilder.addParameter(cType)
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
            irReturn(kotlinToBridged(expression))
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

private class StructValuePassing(private val kotlinClass: IrClass, private val cType: CType) : ValuePassing {
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

private class ObjCReferenceValuePassing(private val symbols: KonanSymbols, private val type: IrType) : SimpleValuePassing() {
    override val kotlinBridgeType: IrType
        get() = symbols.nativePtrType
    override val cBridgeType: CType
        get() = CTypes.voidPtr
    override val cType: CType
        get() = CTypes.voidPtr

    override fun IrBuilderWithScope.kotlinToBridged(expression: IrExpression): IrExpression =
            irCall(symbols.interopObjCObjectRawValueGetter.owner).apply {
                extensionReceiver = expression
            }

    override fun IrBuilderWithScope.bridgedToKotlin(expression: IrExpression, symbols: KonanSymbols): IrExpression =
            irCall(symbols.interopInterpretObjCPointerOrNull, listOf(type)).apply {
                putValueArgument(0, expression)
            }

    override fun bridgedToC(expression: String): String = expression
    override fun cToBridged(expression: String): String = expression

}

private class WCStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val wcstr = irBuilder.irCall(symbols.interopWcstr.owner).apply {
            extensionReceiver = expression
        }
        return with(CValuesRefArgumentPassing) { passValue(wcstr) }
    }

}

private class CStringArgumentPassing : KotlinToCArgumentPassing {

    override fun KotlinToCCallBuilder.passValue(expression: IrExpression): CExpression {
        val cstr = irBuilder.irCall(symbols.interopCstr.owner).apply {
            extensionReceiver = expression
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
} else with(irBuilder) {
    val getPointerFunction = symbols.interopCValuesRef.owner
            .simpleFunctions()
            .single { it.name.asString() == "getPointer" }

    fun getPointer(expression: IrExpression) = irCall(getPointerFunction).apply {
        dispatchReceiver = expression
        putValueArgument(0, bridgeCallBuilder.getMemScope())
    }

    if (!value.type.isNullable()) {
        getPointer(value) // Optimization
    } else irLetS(value) { valueVarSymbol ->
        val valueVar = valueVarSymbol.owner
        irIfThenElse(
                type = symbols.interopCPointer.typeWithStarProjections.makeNullable(),
                condition = irEqeqeq(irGet(valueVar), irNull()),
                thenPart = irNull(),
                elsePart = getPointer(irGet(valueVar))
        )
    }
}

private object VoidReturning : ValueReturning {
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

internal fun CType.cast(expression: String): String = "((${this.render("")})$expression)"

private fun KotlinStubs.reportUnsupportedType(reason: String, type: IrType, location: TypeLocation): Nothing {
    val typeLocation: String = when (location) {
        is TypeLocation.FunctionArgument -> ""
        is TypeLocation.FunctionCallResult -> " of return value"
        is TypeLocation.FunctionPointerParameter -> " of callback parameter ${location.index + 1}"
        is TypeLocation.FunctionPointerReturnValue -> " of callback return value"
    }

    reportError(location.element, "type ${type.toKotlinType()}$typeLocation is not supported here" +
            if (reason.isNotEmpty()) ": $reason" else "")
}
