// TARGET_BACKEND: JVM
// WITH_STDLIB
// IGNORE_BACKEND: JVM

// FILE: test.kt
import c2.*

fun box(): String =
    C2().b()()

// FILE: C1.kt
package c1

open class C1 {
    companion object {
        @JvmStatic
        protected fun test(string: String): String =
            string
    }
}

// FILE: C2.kt
package c2

import c1.*

class C2 : C1() {
    fun b() = { test("OK") }
}
