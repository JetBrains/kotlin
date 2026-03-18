// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: JVM_IR
// WITH_REFLECT

import kotlin.reflect.*

class Delegate {
    operator fun getValue(t: Any?, p: KProperty<*>): String =
        if (p.typeParameters.size == 0) "OK" else "Fail: ${p.typeParameters}"
}

@JvmInline
value class Kla1<T>(val default: T) {
    fun getValue(): String {
        val prop by Delegate()
        return prop
    }
}

fun box(): String = Kla1(42).getValue()
