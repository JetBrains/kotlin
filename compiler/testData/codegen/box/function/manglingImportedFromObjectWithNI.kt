// Issue: KT-35904

// FILE: lib.kt
package test

object A {
    inline fun <T> bar(x: T) = 42
    inline val <T> T.bar2 get() = 42
}

// FILE: main.kt
package test

import test.A.bar
import test.A.bar2

fun <T> T.foo1(): Int = bar(this)
fun <T> T.foo2(): Int = this.bar2

fun box(): String {
    10.foo1()
    10.foo2()

    return "OK"
}