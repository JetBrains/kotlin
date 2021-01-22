// TARGET_BACKEND: JVM
// !LANGUAGE: +InlineClasses
// FILE: A.kt
// KOTLIN_CONFIGURATION_FLAGS: +JVM.USE_OLD_INLINE_CLASSES_MANGLING_SCHEME
package z

inline class Z(val s: String)

class X {
    fun Int.foo(z: Z, value: String = "OK") = value
}

// FILE: B.kt
import z.*

fun box(): String = with(X()) { 1.foo(Z("")) }
