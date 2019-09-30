// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

import kotlin.reflect.typeOf

var x: Int = 1
fun sideEffects() {
    x++
}

inline fun <reified T> foo(): String {
    sideEffects()
    val x = typeOf<T>().toString()
    return x
}

fun box(): String {
    if (foo<kotlin.Unit>() != "Unit")
        return "FAIL"
    return "OK"
}