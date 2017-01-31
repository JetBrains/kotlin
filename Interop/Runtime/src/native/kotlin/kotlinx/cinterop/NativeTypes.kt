package kotlinx.cinterop

import konan.internal.Intrinsic

class NativePtr private constructor() {
    @Intrinsic external operator fun plus(offset: Long): NativePtr
}

inline val nativeNullPtr: NativePtr
    get() = getNativeNullPtr()

@Intrinsic external fun getNativeNullPtr(): NativePtr

fun <T : CVariable> typeOf(): CVariable.Type = throw Error("typeOf() is called with erased argument")

/**
 * Returns interpretation of entity with given pointer.
 *
 * @param T must not be abstract
 */
@Intrinsic external fun <T : NativePointed> interpretPointed(ptr: NativePtr): T

inline fun <reified T : CAdaptedFunctionType<*>> CAdaptedFunctionType.Companion.getInstanceOf(): T =
        TODO("CAdaptedFunctionType.getInstanceOf")

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=$rawValue)"