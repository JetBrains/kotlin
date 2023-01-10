// WITH_STDLIB
// MODULE: lib
// FILE: A.kt
package z

inline class Z(val s: String)

class X {
    fun Int.foo(z: Z, value: String = "OK") = value
}

// MODULE: main(lib)
// FILE: B.kt
import z.*

fun box(): String = with(X()) { 1.foo(Z("")) }
