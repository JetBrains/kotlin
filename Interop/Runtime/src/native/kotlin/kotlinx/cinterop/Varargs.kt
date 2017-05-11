package kotlinx.cinterop

private const val MAX_ARGUMENT_SIZE = 8


typealias FfiTypeKind = Int
// Also declared in Interop.cpp
const val FFI_TYPE_KIND_VOID: FfiTypeKind = 0
const val FFI_TYPE_KIND_SINT8: FfiTypeKind = 1
const val FFI_TYPE_KIND_SINT16: FfiTypeKind = 2
const val FFI_TYPE_KIND_SINT32: FfiTypeKind = 3
const val FFI_TYPE_KIND_SINT64: FfiTypeKind = 4
const val FFI_TYPE_KIND_FLOAT: FfiTypeKind = 5
const val FFI_TYPE_KIND_DOUBLE: FfiTypeKind = 6
const val FFI_TYPE_KIND_POINTER: FfiTypeKind = 7

private tailrec fun convertArgument(
        argument: Any?, isVariadic: Boolean, location: COpaquePointer,
        additionalPlacement: NativePlacement
): FfiTypeKind = when (argument) {
    is CValuesRef<*>? -> {
        location.reinterpret<CPointerVar<*>>()[0] = argument?.getPointer(additionalPlacement)
        FFI_TYPE_KIND_POINTER
    }

    is String -> {
        location.reinterpret<CPointerVar<*>>()[0] = argument.cstr.getPointer(additionalPlacement)
        FFI_TYPE_KIND_POINTER
    }

    is Int -> {
        location.reinterpret<IntVar>()[0] = argument
        FFI_TYPE_KIND_SINT32
    }

    is Long -> {
        location.reinterpret<LongVar>()[0] = argument
        FFI_TYPE_KIND_SINT64
    }

    is Byte -> if (isVariadic) {
        convertArgument(argument.toInt(), isVariadic, location, additionalPlacement)
    } else {
        location.reinterpret<ByteVar>()[0] = argument
        FFI_TYPE_KIND_SINT8
    }

    is Short -> if (isVariadic) {
        convertArgument(argument.toInt(), isVariadic, location, additionalPlacement)
    } else {
        location.reinterpret<ShortVar>()[0] = argument
        FFI_TYPE_KIND_SINT16
    }

    is Double -> {
        location.reinterpret<DoubleVar>()[0] = argument
        FFI_TYPE_KIND_DOUBLE
    }

    is Float -> if (isVariadic) {
        convertArgument(argument.toDouble(), isVariadic, location, additionalPlacement)
    } else {
        location.reinterpret<FloatVar>()[0] = argument
        FFI_TYPE_KIND_FLOAT
    }

    is CEnum -> convertArgument(argument.value, isVariadic, location, additionalPlacement)

    else -> throw Error("unsupported argument: $argument")
}

inline fun <reified T  : CVariable> NativePlacement.allocFfiReturnValueBuffer(type: CVariable.Type): T {
    var size = type.size
    var align = type.align

    // libffi requires return value buffer to be no smaller than system register size;
    // TODO: system register size is not exactly the same as pointer size.

    if (size < pointerSize) {
        size = pointerSize.toLong()
    }

    if (align < pointerSize) {
        align = pointerSize
    }

    return this.alloc(size, align).reinterpret<T>()
}

fun callWithVarargs(codePtr: NativePtr, returnValuePtr: NativePtr, returnTypeKind: FfiTypeKind,
                    fixedArguments: Array<out Any?>, variadicArguments: Array<out Any?>,
                    argumentsPlacement: NativePlacement) {

    val totalArgumentsNumber = fixedArguments.size + variadicArguments.size

    // All supported arguments take at most 8 bytes each:
    val argumentsStorage = argumentsPlacement.allocArray<LongVar>(totalArgumentsNumber)
    val arguments = argumentsPlacement.allocArray<CPointerVar<*>>(totalArgumentsNumber)
    val types = argumentsPlacement.allocArray<COpaquePointerVar>(totalArgumentsNumber)

    var index = 0

    inline fun addArgument(argument: Any?, isVariadic: Boolean) {
        val storage = (argumentsStorage + index)!!
        val typeKind = convertArgument(argument, isVariadic = isVariadic,
                location = storage, additionalPlacement = argumentsPlacement)

        types[index] = typeKind.toLong().toCPointer()
        arguments[index] = storage

        ++index
    }

    for (argument in fixedArguments) {
        addArgument(argument, isVariadic = false)
    }

    for (argument in variadicArguments) {
        addArgument(argument, isVariadic = true)
    }

    assert (index == totalArgumentsNumber)

    callWithVarargs(codePtr, returnValuePtr, returnTypeKind, arguments.rawValue, types.rawValue,
            fixedArguments.size, totalArgumentsNumber)
}

@SymbolName("Kotlin_Interop_callWithVarargs")
private external fun callWithVarargs(codePtr: NativePtr, returnValuePtr: NativePtr, returnTypeKind: FfiTypeKind,
                                     arguments: NativePtr, argumentTypeKinds: NativePtr,
                                     fixedArgumentsNumber: Int, totalArgumentsNumber: Int)