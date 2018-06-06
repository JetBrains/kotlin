// IGNORE_BACKEND: JS_IR
abstract class Base(val fn: () -> String)

object Test : Base(run { { Test.ok() } }) {
    fun ok() = "OK"
}

fun box() = Test.fn()