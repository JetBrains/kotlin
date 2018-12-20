// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test


public inline fun <reified T: Any> Any.inlineMeIfYouCan() : () -> Runnable = {
    object : Runnable {
        override fun run() {
            T::class.java.newInstance()
        }
    }
}

// FILE: 2.kt

import test.*

fun box(): String {
    "yo".inlineMeIfYouCan<StringBuilder>()().run()
    return "OK"
}
