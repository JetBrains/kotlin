// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// FILE: 1.kt
// WITH_REFLECT
package test

public inline fun <reified T : Any> inlineMeIfYouCan(): String? =
        {
            f {
                T::class.java.getName()
            }
        }()

inline fun f(x: () -> String) = x()

// FILE: 2.kt

import test.*

class OK

fun box(): String {
    return inlineMeIfYouCan<OK>()!!
}
