// TARGET_BACKEND: JVM
// WITH_RUNTIME
// LANGUAGE_VERSION: 1.2

// FILE: foo.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
package foo.bar

fun f(): String = "O"

val g: String? get() = "K"

inline fun <T> i(block: () -> T): T = block()

// FILE: bar.kt

import foo.bar.*

fun box(): String = i { f() + g }
