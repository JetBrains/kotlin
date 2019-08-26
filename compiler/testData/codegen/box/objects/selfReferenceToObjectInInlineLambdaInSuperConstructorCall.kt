// IGNORE_BACKEND: WASM
abstract class Base(val fn: () -> String)

object Test : Base(run { { Test.ok() } }) {
    fun ok() = "OK"
}

fun box() = Test.fn()