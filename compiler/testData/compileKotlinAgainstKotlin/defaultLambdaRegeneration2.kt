// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: A.kt

package test

inline fun test(s: () -> () -> () -> String = { val z = "Outer"; { { "OK" } } }) =
        s()

val same = test()

// FILE: B.kt

import test.*

fun box(): String {
    val inlined = test()
    if (same::class.java == inlined::class.java) return "fail 1 : ${same::class.java} ==  ${inlined::class.java}"
    if (same()::class.java == inlined()::class.java) return "fail 2 : ${same()::class.java} ==  ${inlined()::class.java}"
    return inlined()()
}
