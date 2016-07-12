// WITH_RUNTIME
// KOTLIN_CONFIGURATION_FLAGS: +JVM.INHERIT_MULTIFILE_PARTS
// FILE: 1.kt

@file:[JvmName("MultifileClass") JvmMultifileClass]
package a

inline fun foo(body: () -> String): String = bar(body())

public fun bar(x: String): String = x

inline fun <reified T> inlineOnly(x: Any?): Boolean = x is T

// FILE: 2.kt

import a.foo
import a.inlineOnly

fun box(): String {
    if (!inlineOnly<String>("OK")) return "fail 1"
   return foo { "OK" }
}
