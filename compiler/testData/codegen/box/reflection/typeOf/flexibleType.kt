// !USE_EXPERIMENTAL: kotlin.ExperimentalStdlibApi
// TARGET_BACKEND: JVM
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_REFLECT
// FILE: box.kt

package test

import kotlin.reflect.*

fun box(): String {
    val v = returnTypeOf { J.get() }.toString()
    if (v != "J") return "Fail: $v"

    return "OK"
}

inline fun <reified T: Any> returnTypeOf(block: () -> T) =
    typeOf<T>()

// FILE: J.java

public class J {
    public static J get() {
        return null;
    }
}
