// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: A.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
@file:JvmName("Facade")
@file:JvmMultifileClass
package foo.bar

typealias S = String

fun f(): String = "O"

val g: S? get() = f().substring(0, 0) + "K"

inline fun <T> i(block: () -> T): T = block()

// FILE: B.kt

import foo.bar.*

fun box(): S = i { f() + g }
