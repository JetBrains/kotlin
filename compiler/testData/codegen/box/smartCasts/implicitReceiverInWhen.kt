// IGNORE_BACKEND: JS_IR
open class A {
    fun f(): String =
            when (this) {
                is B -> x
                else -> "FAIL"
            }
}

class B(val x: String) : A()

fun box() = B("OK").f()
