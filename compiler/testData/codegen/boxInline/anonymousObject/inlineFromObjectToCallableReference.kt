// FILE: 1.kt
package test

inline fun test(crossinline y: () -> String): String =
    object {
        inline fun run(): String = y()
    }.run()

// FILE: 2.kt

import test.*

fun box(): String = (::test) { "OK" }
