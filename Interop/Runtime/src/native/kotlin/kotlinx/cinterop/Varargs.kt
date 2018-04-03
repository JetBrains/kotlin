/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        additionalPlacement: AutofreeScope
): FfiTypeKind = when (argument) {
    is CValuesRef<*>? -> {
        location.reinterpret<CPointerVar<*>>()[0] = argument?.getPointer(additionalPlacement)
        FFI_TYPE_KIND_POINTER
    }

    is String -> {
        location.reinterpret<CPointerVar<*>>()[0] = if (!isVariadic) {
            // If it is fixed argument, then it is not C string because it must have been already converted;
            // then treat it as NSString.
            // TODO: handle fixed NSString arguments in the stub instead.
            interpretCPointer<COpaque>(CreateNSStringFromKString(argument))
        } else {
            // It is passed as variadic argument; no type information available, so treat it as C string.
            argument.cstr.getPointer(additionalPlacement)
        }
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

    is Boolean -> convertArgument(argument.toByte(), isVariadic, location, additionalPlacement)

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

    is ForeignObjCObject -> {
        location.reinterpret<COpaquePointerVar>()[0] = interpretCPointer((argument as ObjCObject).rawPtr())
        FFI_TYPE_KIND_POINTER
    }

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
                    fixedArguments: Array<out Any?>, variadicArguments: Array<out Any?>?,
                    argumentsPlacement: AutofreeScope) {

    val totalArgumentsNumber = fixedArguments.size + if (variadicArguments == null) 0 else variadicArguments.size

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

    val variadicArgumentsNumber: Int

    if (variadicArguments != null) {
        for (argument in variadicArguments) {
            addArgument(argument, isVariadic = true)
        }
        variadicArgumentsNumber = variadicArguments.size
    } else {
        variadicArgumentsNumber = -1
    }

    assert (index == totalArgumentsNumber)

    callFunctionPointer(codePtr, returnValuePtr, returnTypeKind, arguments.rawValue, types.rawValue,
            totalArgumentsNumber, variadicArgumentsNumber)
}

@SymbolName("Kotlin_Interop_callFunctionPointer")
private external fun callFunctionPointer(
        codePtr: NativePtr,
        returnValuePtr: NativePtr,
        returnTypeKind: FfiTypeKind,
        arguments: NativePtr,
        argumentTypeKinds: NativePtr,
        totalArgumentsNumber: Int,
        variadicArgumentsNumber: Int
)
