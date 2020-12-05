// !LANGUAGE: +InlineClasses
// FILE: A.kt
package z

inline class Z(val s: String)

class X {
    fun Int.foo(z: Z, value: String = "OK") = value
}

// FILE: B.kt
import z.*

fun box(): String = with(X()) { 1.foo(Z("")) }
