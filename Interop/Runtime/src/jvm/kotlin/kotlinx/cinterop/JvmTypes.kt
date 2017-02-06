package kotlinx.cinterop

import kotlin.reflect.companionObjectInstance
import kotlin.reflect.primaryConstructor

typealias NativePtr = Long
val nativeNullPtr: NativePtr = 0L

// TODO: the functions below should eventually be intrinsified

inline fun <reified T : CVariable> typeOf() = T::class.companionObjectInstance as CVariable.Type

/**
 * Returns interpretation of entity with given pointer.
 *
 * @param T must not be abstract
 */
inline fun <reified T : NativePointed> interpretPointed(ptr: NativePtr): T {
    return ensuringNotNull(ptr) {
        val kClass = T::class
        val primaryConstructor = kClass.primaryConstructor
        if (primaryConstructor == null) {
            throw IllegalArgumentException("${kClass.simpleName} doesn't have a constructor")
        }
        (primaryConstructor as (NativePtr) -> T)(ptr)
    }
}

inline fun <reified T : CAdaptedFunctionType<*>> CAdaptedFunctionType.Companion.getInstanceOf(): T =
        T::class.objectInstance!!

internal fun CPointer<*>.cPointerToString() = "CPointer(raw=0x%x)".format(rawValue)