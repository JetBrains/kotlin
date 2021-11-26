// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: lib
// FILE: A.kt

package test

inline fun test(s: () -> () -> String = { val z = "Outer"; { "OK" } }) =
        s()

val same = test()

// MODULE: main(lib)
// FILE: B.kt

import test.*

fun box(): String {
    val inlined = test()
    if (same::class.java == inlined::class.java) return "fail 1 : ${same::class.java} ==  ${inlined::class.java}"
    println (inlined::class.java)
    return inlined()
}
