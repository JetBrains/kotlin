// IGNORE_BACKEND: JVM, JVM_IR

// MODULE: lib
// FILE: Z.kt
package z

private interface I {
    fun k(s: String = "K") = s
}

class Z1 : I {
    val o = "O"
}

class Z2 : I {
    val o = "O"
}

// MODULE: main(lib)
// FILE: box.kt
import z.Z1
import z.Z2

fun box(): String {
    val s = Z1().run { o + k() } + Z2().run { o + k() }
    return if (s == "OKOK") "OK" else "Fail: $s"
}