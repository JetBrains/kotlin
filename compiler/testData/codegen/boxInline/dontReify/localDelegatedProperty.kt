
// FILE: 1.kt

import kotlin.reflect.KProperty

inline fun <T> foo(): String {
    val x: T by ""
    return x as String
}

operator fun <T> String.getValue(nothing: Any?, property: KProperty<*>) = "OK" as T

// FILE: 2.kt

fun box() = foo<Int>()