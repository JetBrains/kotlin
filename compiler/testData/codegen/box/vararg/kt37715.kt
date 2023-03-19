// WITH_STDLIB

import kotlin.collections.toList

fun <T: Number> foo(vararg values: T) = values.toList()

fun box(): String {
    val a = foo(1, 4.5)
    return "OK"
}
