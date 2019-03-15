// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test


open class Base
class Child(val value: String): Base()

inline fun inlineFun(s: (Child) -> Base = { a: Base -> a as Child}): Base {
    return s(Child("OK"))
}

// FILE: 2.kt
import test.*

fun box(): String {
    return (inlineFun() as Child).value
}