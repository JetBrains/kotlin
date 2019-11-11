// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// WITH_REFLECT

import kotlin.reflect.typeOf

fun sideEffects() {
    println("Side effect")
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