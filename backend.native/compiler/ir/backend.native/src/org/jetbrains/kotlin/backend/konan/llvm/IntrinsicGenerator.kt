package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.cValuesOf
import llvm.*
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.descriptors.isTypedIntrinsic
import org.jetbrains.kotlin.backend.konan.llvm.objc.genObjCSelector
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.isSuspend

internal enum class IntrinsicType {
    PLUS,
    MINUS,
    TIMES,
    SIGNED_DIV,
    SIGNED_REM,
    UNSIGNED_DIV,
    UNSIGNED_REM,
    INC,
    DEC,
    UNARY_PLUS,
    UNARY_MINUS,
    SHL,
    SHR,
    USHR,
    AND,
    OR,
    XOR,
    INV,
    SIGN_EXTEND,
    ZERO_EXTEND,
    INT_TRUNCATE,
    FLOAT_TRUNCATE,
    FLOAT_EXTEND,
    SIGNED_TO_FLOAT,
    UNSIGNED_TO_FLOAT,
    FLOAT_TO_SIGNED,
    SIGNED_COMPARE_TO,
    UNSIGNED_COMPARE_TO,
    NOT,
    REINTERPRET,
    EXTRACT_ELEMENT,
    ARE_EQUAL_BY_VALUE,
    IEEE_754_EQUALS,
    // OBJC
    OBJC_GET_MESSENGER,
    OBJC_GET_MESSENGER_STRET,
    OBJC_GET_OBJC_CLASS,
    OBJC_CREATE_SUPER_STRUCT,
    OBJC_INIT_BY,
    OBJC_GET_SELECTOR,
    // Other
    GET_CLASS_TYPE_INFO,
    CREATE_UNINITIALIZED_INSTANCE,
    LIST_OF_INTERNAL,
    IDENTITY,
    IMMUTABLE_BLOB,
    INIT_INSTANCE,
    // Enums
    ENUM_VALUES,
    ENUM_VALUE_OF,
    // Coroutines
    GET_CONTINUATION,
    RETURN_IF_SUSPENDED,
    COROUTINE_LAUNCHPAD,
    // Interop
    INTEROP_READ_BITS,
    INTEROP_WRITE_BITS,
    INTEROP_READ_PRIMITIVE,
    INTEROP_WRITE_PRIMITIVE,
    INTEROP_GET_POINTER_SIZE,
    INTEROP_NATIVE_PTR_TO_LONG,
    INTEROP_NATIVE_PTR_PLUS_LONG,
    INTEROP_GET_NATIVE_NULL_PTR,
    INTEROP_CONVERT,
    INTEROP_BITS_TO_FLOAT,
    INTEROP_BITS_TO_DOUBLE,
    INTEROP_SIGN_EXTEND,
    INTEROP_NARROW,
    INTEROP_STATIC_C_FUNCTION,
    INTEROP_FUNPTR_INVOKE,
    INTEROP_MEMORY_COPY,
    // Worker
    WORKER_EXECUTE
}

// Explicit and single interface between Intrinsic Generator and IrToBitcode.
internal interface IntrinsicGeneratorEnvironment {

    val codegen: CodeGenerator

    val functionGenerationContext: FunctionGenerationContext

    val continuation: LLVMValueRef

    val exceptionHandler: ExceptionHandler

    fun calculateLifetime(element: IrElement): Lifetime

    fun evaluateCall(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime, superClass: IrClass? = null): LLVMValueRef

    fun evaluateExplicitArgs(expression: IrFunctionAccessExpression): List<LLVMValueRef>

    fun evaluateExpression(value: IrExpression): LLVMValueRef
}

internal fun tryGetIntrinsicType(callSite: IrFunctionAccessExpression): IntrinsicType? =
        if (callSite.symbol.owner.isTypedIntrinsic) getIntrinsicType(callSite) else null

private fun getIntrinsicType(callSite: IrFunctionAccessExpression): IntrinsicType {
    val function = callSite.symbol.owner
    val annotation = function.annotations.findAnnotation(RuntimeNames.typedIntrinsicAnnotation)!!
    val value = annotation.getAnnotationStringValue()!!
    return IntrinsicType.valueOf(value)
}

internal class IntrinsicGenerator(private val environment: IntrinsicGeneratorEnvironment) {

    private val codegen = environment.codegen

    private val context = codegen.context

    private val IrCall.llvmReturnType: LLVMTypeRef
        get() = LLVMGetReturnType(codegen.getLlvmFunctionType(symbol.owner))!!


    private fun LLVMTypeRef.sizeInBits() = LLVMSizeOfTypeInBits(codegen.llvmTargetData, this).toInt()

    /**
     * Some intrinsics have to be processed before evaluation of their arguments.
     * So this method looks at [callSite] and if it is call to "special" intrinsic
     * processes it. Otherwise it returns null.
     */
    fun tryEvaluateSpecialCall(callSite: IrFunctionAccessExpression): LLVMValueRef? {
        val function = callSite.symbol.owner
        if (!function.isTypedIntrinsic) {
            return null
        }
        val intrinsicType = getIntrinsicType(callSite)
        return when (intrinsicType) {
            IntrinsicType.IMMUTABLE_BLOB -> {
                @Suppress("UNCHECKED_CAST")
                val arg = callSite.getValueArgument(0) as IrConst<String>
                context.llvm.staticData.createImmutableBlob(arg)
            }
            IntrinsicType.OBJC_INIT_BY -> {
                val receiver = environment.evaluateExpression(callSite.extensionReceiver!!)
                val irConstructorCall = callSite.getValueArgument(0) as IrConstructorCall
                val constructorDescriptor = irConstructorCall.symbol.owner
                val constructorArgs = environment.evaluateExplicitArgs(irConstructorCall)
                val args = listOf(receiver) + constructorArgs
                environment.evaluateCall(constructorDescriptor, args, Lifetime.IRRELEVANT)
                receiver
            }
            IntrinsicType.OBJC_GET_SELECTOR -> {
                val selector = (callSite.getValueArgument(0) as IrConst<*>).value as String
                environment.functionGenerationContext.genObjCSelector(selector)
            }
            IntrinsicType.INIT_INSTANCE -> {
                val initializer = callSite.getValueArgument(1) as IrConstructorCall
                val thiz = environment.evaluateExpression(callSite.getValueArgument(0)!!)
                environment.evaluateCall(
                        initializer.symbol.owner,
                        listOf(thiz) + environment.evaluateExplicitArgs(initializer),
                        environment.calculateLifetime(initializer)
                )
                codegen.theUnitInstanceRef.llvm
            }
            IntrinsicType.COROUTINE_LAUNCHPAD -> {
                val suspendFunctionCall = callSite.getValueArgument(0) as IrCall
                val continuation = environment.evaluateExpression(callSite.getValueArgument(1)!!)
                val suspendFunction = suspendFunctionCall.symbol.owner
                assert(suspendFunction.isSuspend) { "Call to a suspend function expected but was ${suspendFunction.dump()}" }
                environment.evaluateCall(suspendFunction,
                        environment.evaluateExplicitArgs(suspendFunctionCall) + listOf(continuation),
                        environment.calculateLifetime(suspendFunctionCall),
                        suspendFunction.parent as? IrClass // Call non-virtually.
                )
            }
            else -> null
        }
    }

    fun evaluateCall(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef =
            environment.functionGenerationContext.evaluateCall(callSite, args)

    // Assuming that we checked for `TypedIntrinsic` annotation presence.
    private fun FunctionGenerationContext.evaluateCall(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef =
            when (val intrinsicType = getIntrinsicType(callSite)) {
                IntrinsicType.PLUS -> emitPlus(args)
                IntrinsicType.MINUS -> emitMinus(args)
                IntrinsicType.TIMES -> emitTimes(args)
                IntrinsicType.SIGNED_DIV -> emitSignedDiv(args)
                IntrinsicType.SIGNED_REM -> emitSignedRem(args)
                IntrinsicType.UNSIGNED_DIV -> emitUnsignedDiv(args)
                IntrinsicType.UNSIGNED_REM -> emitUnsignedRem(args)
                IntrinsicType.INC -> emitInc(args)
                IntrinsicType.DEC -> emitDec(args)
                IntrinsicType.UNARY_PLUS -> emitUnaryPlus(args)
                IntrinsicType.UNARY_MINUS -> emitUnaryMinus(args)
                IntrinsicType.SHL -> emitShl(args)
                IntrinsicType.SHR -> emitShr(args)
                IntrinsicType.USHR -> emitUshr(args)
                IntrinsicType.AND -> emitAnd(args)
                IntrinsicType.OR -> emitOr(args)
                IntrinsicType.XOR -> emitXor(args)
                IntrinsicType.INV -> emitInv(args)
                IntrinsicType.SIGNED_COMPARE_TO -> emitSignedCompareTo(args)
                IntrinsicType.UNSIGNED_COMPARE_TO -> emitUnsignedCompareTo(args)
                IntrinsicType.NOT -> emitNot(args)
                IntrinsicType.REINTERPRET -> emitReinterpret(callSite, args)
                IntrinsicType.EXTRACT_ELEMENT -> emitExtractElement(callSite, args)
                IntrinsicType.SIGN_EXTEND -> emitSignExtend(callSite, args)
                IntrinsicType.ZERO_EXTEND -> emitZeroExtend(callSite, args)
                IntrinsicType.INT_TRUNCATE -> emitIntTruncate(callSite, args)
                IntrinsicType.SIGNED_TO_FLOAT -> emitSignedToFloat(callSite, args)
                IntrinsicType.UNSIGNED_TO_FLOAT -> emitUnsignedToFloat(callSite, args)
                IntrinsicType.FLOAT_TO_SIGNED -> emitFloatToSigned(callSite, args)
                IntrinsicType.FLOAT_EXTEND -> emitFloatExtend(callSite, args)
                IntrinsicType.FLOAT_TRUNCATE -> emitFloatTruncate(callSite, args)
                IntrinsicType.ARE_EQUAL_BY_VALUE -> emitAreEqualByValue(args)
                IntrinsicType.IEEE_754_EQUALS -> emitIeee754Equals(args)
                IntrinsicType.OBJC_GET_MESSENGER -> emitObjCGetMessenger(args, isStret = false)
                IntrinsicType.OBJC_GET_MESSENGER_STRET -> emitObjCGetMessenger(args, isStret = true)
                IntrinsicType.OBJC_GET_OBJC_CLASS -> emitGetObjCClass(callSite)
                IntrinsicType.OBJC_CREATE_SUPER_STRUCT -> emitObjCCreateSuperStruct(args)
                IntrinsicType.GET_CLASS_TYPE_INFO -> emitGetClassTypeInfo(callSite)
                IntrinsicType.INTEROP_READ_BITS -> emitReadBits(args)
                IntrinsicType.INTEROP_WRITE_BITS -> emitWriteBits(args)
                IntrinsicType.INTEROP_READ_PRIMITIVE -> emitReadPrimitive(callSite, args)
                IntrinsicType.INTEROP_WRITE_PRIMITIVE -> emitWritePrimitive(callSite, args)
                IntrinsicType.INTEROP_GET_POINTER_SIZE -> emitGetPointerSize()
                IntrinsicType.CREATE_UNINITIALIZED_INSTANCE -> emitCreateUninitializedInstance(callSite)
                IntrinsicType.INTEROP_NATIVE_PTR_TO_LONG -> emitNativePtrToLong(callSite, args)
                IntrinsicType.INTEROP_NATIVE_PTR_PLUS_LONG -> emitNativePtrPlusLong(args)
                IntrinsicType.INTEROP_GET_NATIVE_NULL_PTR -> emitGetNativeNullPtr()
                IntrinsicType.LIST_OF_INTERNAL -> emitListOfInternal(callSite, args)
                IntrinsicType.IDENTITY -> emitIdentity(args)
                IntrinsicType.GET_CONTINUATION -> emitGetContinuation()
                IntrinsicType.INTEROP_MEMORY_COPY -> emitMemoryCopy(callSite, args)
                IntrinsicType.RETURN_IF_SUSPENDED,
                IntrinsicType.INTEROP_BITS_TO_FLOAT,
                IntrinsicType.INTEROP_BITS_TO_DOUBLE,
                IntrinsicType.INTEROP_SIGN_EXTEND,
                IntrinsicType.INTEROP_NARROW,
                IntrinsicType.INTEROP_STATIC_C_FUNCTION,
                IntrinsicType.INTEROP_FUNPTR_INVOKE,
                IntrinsicType.INTEROP_CONVERT,
                IntrinsicType.ENUM_VALUES,
                IntrinsicType.ENUM_VALUE_OF,
                IntrinsicType.WORKER_EXECUTE ->
                    reportNonLoweredIntrinsic(intrinsicType)
                IntrinsicType.INIT_INSTANCE,
                IntrinsicType.OBJC_INIT_BY,
                IntrinsicType.COROUTINE_LAUNCHPAD,
                IntrinsicType.OBJC_GET_SELECTOR,
                IntrinsicType.IMMUTABLE_BLOB ->
                    reportSpecialIntrinsic(intrinsicType)
            }

    private fun reportSpecialIntrinsic(intrinsicType: IntrinsicType): Nothing =
            context.reportCompilationError("$intrinsicType should be handled by `tryEvaluateSpecialCall`")

    private fun reportNonLoweredIntrinsic(intrinsicType: IntrinsicType): Nothing =
            context.reportCompilationError("Intrinsic of type $intrinsicType should be handled by previos lowering phase")

    private fun FunctionGenerationContext.emitGetContinuation(): LLVMValueRef =
            environment.continuation

    private fun FunctionGenerationContext.emitIdentity(args: List<LLVMValueRef>): LLVMValueRef =
            args.single()

    private fun FunctionGenerationContext.emitListOfInternal(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val varargExpression = callSite.getValueArgument(0) as IrVararg
        val vararg = args.single()

        val length = varargExpression.elements.size
        // TODO: store length in `vararg` itself when more abstract types will be used for values.

        val array = constPointer(vararg)
        // Note: dirty hack here: `vararg` has type `Array<out E>`, but `createConstArrayList` expects `Array<E>`;
        // however `vararg` is immutable, and in current implementation it has type `Array<E>`,
        // so let's ignore this mismatch currently for simplicity.

        return context.llvm.staticData.createConstArrayList(array, length).llvm
    }

    private fun FunctionGenerationContext.emitGetNativeNullPtr(): LLVMValueRef =
            kNullInt8Ptr

    private fun FunctionGenerationContext.emitNativePtrPlusLong(args: List<LLVMValueRef>): LLVMValueRef =
        gep(args[0], args[1])

    private fun FunctionGenerationContext.emitNativePtrToLong(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val intPtrValue = ptrToInt(args.single(), codegen.intPtrType)
        val resultType = callSite.llvmReturnType
        return if (resultType == intPtrValue.type) {
            intPtrValue
        } else {
            LLVMBuildSExt(builder, intPtrValue, resultType, "")!!
        }
    }

    private fun FunctionGenerationContext.emitCreateUninitializedInstance(callSite: IrCall): LLVMValueRef {
        val typeParameterT = context.ir.symbols.createUninitializedInstance.descriptor.typeParameters[0]
        val enumClass = callSite.getTypeArgument(typeParameterT)!!
        val enumIrClass = enumClass.getClass()!!
        return allocInstance(enumIrClass, environment.calculateLifetime(callSite))
    }

    private fun FunctionGenerationContext.emitGetPointerSize(): LLVMValueRef =
            Int32(LLVMPointerSize(codegen.llvmTargetData)).llvm

    private fun FunctionGenerationContext.emitReadPrimitive(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val pointerType = pointerType(callSite.llvmReturnType)
        val rawPointer = args.last()
        val pointer = bitcast(pointerType, rawPointer)
        return load(pointer)
    }

    private fun FunctionGenerationContext.emitWritePrimitive(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val function = callSite.symbol.owner
        val pointerType = pointerType(codegen.getLLVMType(function.valueParameters.last().type))
        val rawPointer = args[1]
        val pointer = bitcast(pointerType, rawPointer)
        store(args[2], pointer)
        return codegen.theUnitInstanceRef.llvm
    }

    private fun FunctionGenerationContext.emitReadBits(args: List<LLVMValueRef>): LLVMValueRef {
        val ptr = args[0]
        assert(ptr.type == int8TypePtr)

        val offset = extractConstUnsignedInt(args[1])
        val size = extractConstUnsignedInt(args[2]).toInt()
        val signed = extractConstUnsignedInt(args[3]) != 0L

        val prefixBitsNum = (offset % 8).toInt()
        val suffixBitsNum = (8 - ((size + offset) % 8).toInt()) % 8

        // Note: LLVM allows to read without padding tail up to byte boundary, but the result seems to be incorrect.

        val bitsWithPaddingNum = prefixBitsNum + size + suffixBitsNum
        val bitsWithPaddingType = LLVMIntTypeInContext(llvmContext, bitsWithPaddingNum)!!

        val bitsWithPaddingPtr = bitcast(org.jetbrains.kotlin.backend.konan.llvm.pointerType(bitsWithPaddingType), gep(ptr, org.jetbrains.kotlin.backend.konan.llvm.Int64(offset / 8).llvm))
        val bitsWithPadding = load(bitsWithPaddingPtr).setUnaligned()

        val bits = shr(
                shl(bitsWithPadding, suffixBitsNum),
                prefixBitsNum + suffixBitsNum, signed
        )
        return when {
            bitsWithPaddingNum == 64 -> bits
            bitsWithPaddingNum > 64 -> trunc(bits, org.jetbrains.kotlin.backend.konan.llvm.int64Type)
            else -> ext(bits, org.jetbrains.kotlin.backend.konan.llvm.int64Type, signed)
        }
    }

    private fun FunctionGenerationContext.emitWriteBits(args: List<LLVMValueRef>): LLVMValueRef {
        val ptr = args[0]
        assert(ptr.type == int8TypePtr)

        val offset = extractConstUnsignedInt(args[1])
        val size = extractConstUnsignedInt(args[2]).toInt()

        val value = args[3]
        assert(value.type == int64Type)

        val bitsType = LLVMIntTypeInContext(llvmContext, size)!!

        val prefixBitsNum = (offset % 8).toInt()
        val suffixBitsNum = (8 - ((size + offset) % 8).toInt()) % 8

        val bitsWithPaddingNum = prefixBitsNum + size + suffixBitsNum
        val bitsWithPaddingType = LLVMIntTypeInContext(llvmContext, bitsWithPaddingNum)!!

        // 0011111000:
        val discardBitsMask = LLVMConstShl(
                LLVMConstZExt(
                        LLVMConstAllOnes(bitsType), // 11111
                        bitsWithPaddingType
                ), // 1111100000
                LLVMConstInt(bitsWithPaddingType, prefixBitsNum.toLong(), 0)
        )

        val preservedBitsMask = LLVMConstNot(discardBitsMask)!!

        val bitsWithPaddingPtr = bitcast(pointerType(bitsWithPaddingType), gep(ptr, Int64(offset / 8).llvm))

        val bits = trunc(value, bitsType)

        val bitsToStore = if (prefixBitsNum == 0 && suffixBitsNum == 0) {
            bits
        } else {
            val previousValue = load(bitsWithPaddingPtr).setUnaligned()
            val preservedBits = and(previousValue, preservedBitsMask)
            val bitsWithPadding = shl(zext(bits, bitsWithPaddingType), prefixBitsNum)

            or(bitsWithPadding, preservedBits)
        }
        llvm.LLVMBuildStore(builder, bitsToStore, bitsWithPaddingPtr)!!.setUnaligned()
        return codegen.theUnitInstanceRef.llvm
    }

    private fun FunctionGenerationContext.emitMemoryCopy(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        println("memcpy at ${callSite}")
        args.map { println(llvm2string(it)) }
        TODO("Implement me")
    }

    private fun FunctionGenerationContext.emitGetClassTypeInfo(callSite: IrCall): LLVMValueRef {
        val typeArgument = callSite.getTypeArgument(0)!!
        val typeArgumentClass = typeArgument.getClass()
        return if (typeArgumentClass == null) {
            // Should not happen anymore, but it is safer to handle this case.
            unreachable()
            kNullInt8Ptr
        } else {
            val typeInfo = codegen.typeInfoValue(typeArgumentClass)
            LLVMConstBitCast(typeInfo, kInt8Ptr)!!
        }
    }

    private fun FunctionGenerationContext.emitObjCCreateSuperStruct(args: List<LLVMValueRef>): LLVMValueRef {
        assert(args.size == 2)
        val receiver = args[0]
        val superClass = args[1]

        val structType = structType(kInt8Ptr, kInt8Ptr)
        val ptr = alloca(structType)
        store(receiver, LLVMBuildGEP(builder, ptr, cValuesOf(kImmZero, kImmZero), 2, "")!!)
        store(superClass, LLVMBuildGEP(builder, ptr, cValuesOf(kImmZero, kImmOne), 2, "")!!)
        return bitcast(int8TypePtr, ptr)
    }

    // TODO: Find better place for these guys.
    private val kImmZero     = LLVMConstInt(int32Type,  0, 1)!!
    private val kImmOne      = LLVMConstInt(int32Type,  1, 1)!!

    private fun FunctionGenerationContext.emitGetObjCClass(callSite: IrCall): LLVMValueRef {
        val typeArgument = callSite.getTypeArgument(0)
        return getObjCClass(typeArgument!!.getClass()!!, environment.exceptionHandler)
    }

    private fun FunctionGenerationContext.emitObjCGetMessenger(args: List<LLVMValueRef>, isStret: Boolean): LLVMValueRef {
        val messengerNameSuffix = if (isStret) "_stret" else ""

        val functionType = functionType(int8TypePtr, true, int8TypePtr, int8TypePtr)

        val libobjc = context.standardLlvmSymbolsOrigin
        val normalMessenger = context.llvm.externalFunction(
                "objc_msgSend$messengerNameSuffix",
                functionType,
                origin = libobjc
        )
        val superMessenger = context.llvm.externalFunction(
                "objc_msgSendSuper$messengerNameSuffix",
                functionType,
                origin = libobjc
        )

        val superClass = args.single()
        val messenger = LLVMBuildSelect(builder,
                If = icmpEq(superClass, kNullInt8Ptr),
                Then = normalMessenger,
                Else = superMessenger,
                Name = ""
        )!!

        return bitcast(int8TypePtr, messenger)
    }

    private fun FunctionGenerationContext.emitAreEqualByValue(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        assert (first.type == second.type) { "Types are different: '${llvmtype2string(first.type)}' and '${llvmtype2string(second.type)}'" }

        return when (val typeKind = LLVMGetTypeKind(first.type)) {
            llvm.LLVMTypeKind.LLVMFloatTypeKind, llvm.LLVMTypeKind.LLVMDoubleTypeKind,
            LLVMTypeKind.LLVMVectorTypeKind -> {
                // TODO LLVM API does not provide guarantee for LLVMIntTypeInContext availability for longer types; consider meaningful diag message instead of NPE
                val integerType = LLVMIntTypeInContext(llvmContext, first.type.sizeInBits())!!
                icmpEq(bitcast(integerType, first), bitcast(integerType, second))
            }
            llvm.LLVMTypeKind.LLVMIntegerTypeKind, llvm.LLVMTypeKind.LLVMPointerTypeKind -> icmpEq(first, second)
            else -> error(typeKind)
        }
    }

    private fun FunctionGenerationContext.emitIeee754Equals(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        assert (first.type == second.type)
                { "Types are different: '${llvmtype2string(first.type)}' and '${llvmtype2string(second.type)}'" }
        val type = LLVMGetTypeKind(first.type)
        assert (type == LLVMTypeKind.LLVMFloatTypeKind || type == LLVMTypeKind.LLVMDoubleTypeKind)
                { "Should be of floating point kind, not: '${llvmtype2string(first.type)}'"}
        return fcmpEq(first, second)
    }

    private fun FunctionGenerationContext.emitReinterpret(callSite: IrCall, args: List<LLVMValueRef>) =
            bitcast(callSite.llvmReturnType, args[0])

    private fun FunctionGenerationContext.emitExtractElement(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val (vector, index) = args
        val elementSize = LLVMSizeOfTypeInBits(codegen.llvmTargetData, callSite.llvmReturnType).toInt()
        val vectorSize = LLVMSizeOfTypeInBits(codegen.llvmTargetData, vector.type).toInt()

        assert(callSite.llvmReturnType.isVectorElementType()
                && vectorSize % elementSize == 0
        ) { "Invalid vector element type ${LLVMGetTypeKind(callSite.llvmReturnType)}"}

        val elementCount = vectorSize / elementSize
        emitThrowIfOOB(index, Int32((elementCount)).llvm)

        val targetType = LLVMVectorType(callSite.llvmReturnType, elementCount)!!
        return extractElement(
                (if (targetType == vector.type) vector else bitcast(targetType, vector)),
                index)
    }

    private fun FunctionGenerationContext.emitNot(args: List<LLVMValueRef>) =
            not(args[0])

    private fun FunctionGenerationContext.emitPlus(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return if (first.type.isFloatingPoint()) {
            fadd(first, second)
        } else {
            add(first, second)
        }
    }

    private fun FunctionGenerationContext.emitSignExtend(callSite: IrCall, args: List<LLVMValueRef>) =
            sext(args[0], callSite.llvmReturnType)

    private fun FunctionGenerationContext.emitZeroExtend(callSite: IrCall, args: List<LLVMValueRef>) =
            zext(args[0], callSite.llvmReturnType)

    private fun FunctionGenerationContext.emitIntTruncate(callSite: IrCall, args: List<LLVMValueRef>) =
            trunc(args[0], callSite.llvmReturnType)

    private fun FunctionGenerationContext.emitSignedToFloat(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildSIToFP(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitUnsignedToFloat(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildUIToFP(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitFloatToSigned(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildFPToSI(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitFloatExtend(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildFPExt(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitFloatTruncate(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildFPTrunc(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitShift(op: LLVMOpcode, args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        val shift = if (first.type == int64Type) {
            val tmp = and(second, Int32(63).llvm)
            zext(tmp, int64Type)
        } else {
            and(second, Int32(31).llvm)
        }
        return LLVMBuildBinOp(builder, op, first, shift, "")!!
    }

    private fun FunctionGenerationContext.emitShl(args: List<LLVMValueRef>) =
            emitShift(LLVMOpcode.LLVMShl, args)

    private fun FunctionGenerationContext.emitShr(args: List<LLVMValueRef>) =
            emitShift(LLVMOpcode.LLVMAShr, args)

    private fun FunctionGenerationContext.emitUshr(args: List<LLVMValueRef>) =
            emitShift(LLVMOpcode.LLVMLShr, args)

    private fun FunctionGenerationContext.emitAnd(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return and(first, second)
    }

    private fun FunctionGenerationContext.emitOr(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return or(first, second)
    }

    private fun FunctionGenerationContext.emitXor(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return xor(first, second)
    }

    private fun FunctionGenerationContext.emitInv(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val mask = makeConstOfType(first.type, -1)
        return xor(first, mask)
    }

    private fun FunctionGenerationContext.emitMinus(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return if (first.type.isFloatingPoint()) {
            fsub(first, second)
        } else {
            sub(first, second)
        }
    }

    private fun FunctionGenerationContext.emitTimes(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return if (first.type.isFloatingPoint()) {
            LLVMBuildFMul(builder, first, second, "")
        } else {
            LLVMBuildMul(builder, first, second, "")
        }!!
    }

    private fun FunctionGenerationContext.emitThrowIfZero(divider: LLVMValueRef) {
        ifThen(icmpEq(divider, Zero(divider.type).llvm)) {
            val throwArithExc = codegen.llvmFunction(context.ir.symbols.throwArithmeticException.owner)
            call(throwArithExc, emptyList(), Lifetime.GLOBAL, environment.exceptionHandler)
            unreachable()
        }
    }

    private fun FunctionGenerationContext.emitThrowIfOOB(index: LLVMValueRef, size: LLVMValueRef) {
        ifThen(icmpUGe(index, size)) {
            val throwIndexOutOfBoundsException = codegen.llvmFunction(context.ir.symbols.throwIndexOutOfBoundsException.owner)
            call(throwIndexOutOfBoundsException, emptyList(), Lifetime.GLOBAL, environment.exceptionHandler)
            unreachable()
        }
    }

    private fun FunctionGenerationContext.emitSignedDiv(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        if (!second.type.isFloatingPoint()) {
            emitThrowIfZero(second)
        }
        return if (first.type.isFloatingPoint()) {
            LLVMBuildFDiv(builder, first, second, "")
        } else {
            LLVMBuildSDiv(builder, first, second, "")
        }!!
    }

    private fun FunctionGenerationContext.emitSignedRem(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        if (!second.type.isFloatingPoint()) {
            emitThrowIfZero(second)
        }
        return if (first.type.isFloatingPoint()) {
            LLVMBuildFRem(builder, first, second, "")
        } else {
            LLVMBuildSRem(builder, first, second, "")
        }!!
    }

    private fun FunctionGenerationContext.emitUnsignedDiv(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        emitThrowIfZero(second)
        return LLVMBuildUDiv(builder, first, second, "")!!
    }

    private fun FunctionGenerationContext.emitUnsignedRem(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        emitThrowIfZero(second)
        return LLVMBuildURem(builder, first, second, "")!!
    }

    private fun FunctionGenerationContext.emitInc(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val const1 = makeConstOfType(first.type, 1)
        return if (first.type.isFloatingPoint()) {
            fadd(first, const1)
        } else {
            add(first, const1)
        }
    }

    private fun FunctionGenerationContext.emitDec(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val const1 = makeConstOfType(first.type, 1)
        return if (first.type.isFloatingPoint()) {
            fsub(first, const1)
        } else {
            sub(first, const1)
        }
    }

    private fun FunctionGenerationContext.emitUnaryPlus(args: List<LLVMValueRef>) =
            args[0]

    private fun FunctionGenerationContext.emitUnaryMinus(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val destTy = first.type
        return if (destTy.isFloatingPoint()) {
            fneg(first)
        } else {
            val const0 = makeConstOfType(destTy, 0)
            sub(const0, first)
        }
    }

    private fun FunctionGenerationContext.emitCompareTo(args: List<LLVMValueRef>, signed: Boolean): LLVMValueRef {
        val (first, second) = args
        val equal = icmpEq(first, second)
        val less = if (signed) icmpLt(first, second) else icmpULt(first, second)
        val tmp = select(less, Int32(-1).llvm, Int32(1).llvm)
        return select(equal, Int32(0).llvm, tmp)
    }

    private fun FunctionGenerationContext.emitSignedCompareTo(args: List<LLVMValueRef>) =
            emitCompareTo(args, signed = true)

    private fun FunctionGenerationContext.emitUnsignedCompareTo(args: List<LLVMValueRef>) =
            emitCompareTo(args, signed = false)

    private fun makeConstOfType(type: LLVMTypeRef, value: Int): LLVMValueRef = when (type) {
        int8Type -> Int8(value.toByte()).llvm
        int16Type -> Char16(value.toChar()).llvm
        int32Type -> Int32(value).llvm
        int64Type -> Int64(value.toLong()).llvm
        floatType -> Float32(value.toFloat()).llvm
        doubleType -> Float64(value.toDouble()).llvm
        else -> context.reportCompilationError("Unexpected primitive type: $type")
    }
}