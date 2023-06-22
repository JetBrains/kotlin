// TARGET_BACKEND: JVM
// IGNORE_BACKEND: JVM
// WITH_REFLECT
// FILE: 1.kt
package test

import kotlin.reflect.*

inline operator fun String.getValue(t:Any?, p: KProperty<*>): String =
    if (p.returnType.classifier == String::class) this else "fail"

inline fun foo(crossinline f: () -> String) = {
    val x by f()
    x
}.let { it() }

// FILE: 2.kt
import test.*

fun box() = foo { "OK" }
