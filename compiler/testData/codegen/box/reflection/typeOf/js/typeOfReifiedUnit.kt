// TARGET_BACKEND: JS_IR
// WITH_REFLECT
// KJS_WITH_FULL_RUNTIME

// FILE: lib.kt
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

// FILE: main.kt
fun box(): String {
    if (foo<kotlin.Unit>() != "Unit")
        return "FAIL"
    return "OK"
}