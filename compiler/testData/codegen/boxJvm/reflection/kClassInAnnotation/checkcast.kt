// TARGET_BACKEND: JVM

// WITH_REFLECT

import kotlin.reflect.KClass

fun box(): String {
    try {
        @Suppress("CAST_NEVER_SUCCEEDS_ERROR")
        String::class.java as KClass<String>
    } catch (e: Exception) {
        return "OK"
    }
    return "fail"
}
