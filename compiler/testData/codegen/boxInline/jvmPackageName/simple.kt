// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND_MULTI_MODULE: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: 1.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
package foo.bar

fun f(): String = "O"

val g: String? get() = "K"

inline fun <T> i(block: () -> T): T = block()

// FILE: 2.kt

import foo.bar.*

fun box(): String = i { f() + g }
