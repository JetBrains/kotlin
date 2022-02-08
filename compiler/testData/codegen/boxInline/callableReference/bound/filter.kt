// WITH_STDLIB
// KJS_WITH_FULL_RUNTIME
// FILE: 1.kt

package test

inline fun stub(f: () -> String): String = f()

// FILE: 2.kt

import test.*

class A(val z: String) {
    fun filter(s: String) = z == s
}


fun box(): String {
    val a = A("OK")
    val s = arrayOf("OK")
    return s.filter(a::filter).first()
}
