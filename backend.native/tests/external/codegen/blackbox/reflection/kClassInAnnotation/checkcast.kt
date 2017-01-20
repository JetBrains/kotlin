// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

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
