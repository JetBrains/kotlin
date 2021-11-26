// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt

package test

class Foo(@JvmField val a: String)

inline fun test(s: () -> String): String {
    return s()
}

// FILE: 2.kt

import test.*

fun box(): String {
    return test(Foo("OK")::a)
}
