// TARGET_BACKEND: JVM
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// WITH_RUNTIME
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