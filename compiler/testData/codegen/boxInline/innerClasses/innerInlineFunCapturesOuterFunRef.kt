// FILE: 1.kt
class E(val x: String) {
    fun bar() = x
    inner class Inner {
        inline fun foo() = this@E::bar
    }
}

// FILE: 2.kt

fun box() = E("OK").Inner().foo()()
