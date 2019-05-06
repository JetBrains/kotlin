// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

open class Base

class Child(val value: String) : Base()

fun foo(a: Base): Child = a as Child


inline fun inlineFun(s: (Child) -> Base = ::foo): Base {
    return s(Child("OK"))
}

// FILE: 2.kt
import test.*

fun box(): String {
    return (inlineFun() as Child).value
}