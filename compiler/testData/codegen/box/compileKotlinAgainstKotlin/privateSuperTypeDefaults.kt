// IGNORE_BACKEND: JVM, JVM_IR

// MODULE: lib
// FILE: Z.kt
package z

private interface I {
    fun k(s: String = "K") = s
}

class Z : I {
    val o = "O"
}

// MODULE: main(lib)
// FILE: box.kt
import z.Z

fun box() = Z().run { o + k() }