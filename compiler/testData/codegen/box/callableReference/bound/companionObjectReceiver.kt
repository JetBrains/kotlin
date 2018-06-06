// IGNORE_BACKEND: JS_IR
class A {
    companion object {
        fun ok() = "OK"
    }
}

fun box() = (A.Companion::ok)()