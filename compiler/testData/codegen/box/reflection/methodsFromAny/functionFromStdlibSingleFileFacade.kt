// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS_IR
// KT-12630 KotlinReflectionInternalError on referencing some functions from stdlib

// IGNORE_BACKEND: JS, NATIVE
// WITH_REFLECT

import kotlin.test.*

fun box(): String {
    val hashCode = Any?::hashCode
    assertEquals("fun kotlin.Any?.hashCode(): kotlin.Int", hashCode.toString())

    return "OK"
}
