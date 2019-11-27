// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.jvm.isAccessible

enum class E

fun box(): String {
    try {
        val c = E::class.constructors.single()
        c.isAccessible = true
        c.call()
        return "Fail: constructing an enum class should not be allowed"
    }
    catch (e: Throwable) {
        return "OK"
    }
}
