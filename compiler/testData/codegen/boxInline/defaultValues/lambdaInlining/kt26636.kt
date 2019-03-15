// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: enumOrThrow$default
package test

enum class TarEnum {
    OK
}
inline fun <reified T : Enum<T>> String?.enumOrNull(): T? {
    this ?: return null
    return enumValues<T>().firstOrNull { it.name == this }
}

inline fun <reified T : Enum<T>> String?.enumOrThrow(handleNull: () -> Throwable = { IllegalArgumentException("Enum type ${T::class.java} not contain value=$this") }): T {
    return this.enumOrNull<T>() ?: throw handleNull()
}

// FILE: 2.kt
import test.*

fun box(): String {
    return "OK".enumOrThrow<TarEnum>()!!.name
}