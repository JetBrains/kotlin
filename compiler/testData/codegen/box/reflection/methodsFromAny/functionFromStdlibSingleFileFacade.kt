// TARGET_BACKEND: JVM
// KT-12630 KotlinReflectionInternalError on referencing some functions from stdlib

// WITH_REFLECT

import kotlin.test.*

fun box(): String {
    val hashCode = Any?::hashCode
    assertEquals("fun kotlin.Any?.hashCode(): kotlin.Int", hashCode.toString())

    return "OK"
}
