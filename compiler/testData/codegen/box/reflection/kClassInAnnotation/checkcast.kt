// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass

fun box(): String {
    try {
        String::class.java as KClass<String>
    } catch (e: Exception) {
        return "OK"
    }
    return "fail"
}
