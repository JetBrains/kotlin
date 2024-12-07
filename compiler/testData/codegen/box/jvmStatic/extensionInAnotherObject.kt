// TARGET_BACKEND: JVM
// WITH_STDLIB
// ISSUE: KT-71551

// MODULE: lib
// FILE: lib.kt
package test

object O {
    @JvmStatic
    public fun String.id(): String = this
}

// MODULE: main(lib)
// FILE: box.kt
import test.O.id

fun box(): String = "OK".id()