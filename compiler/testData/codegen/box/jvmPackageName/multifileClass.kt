// IGNORE_BACKEND: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

// FILE: A1.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
@file:JvmName("Facade")
@file:JvmMultifileClass
package foo.bar

val g: S? get() = f().substring(0, 0) + "K"

// FILE: A2.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
@file:JvmName("Facade")
@file:JvmMultifileClass
package foo.bar

inline fun <T> i(block: () -> T): T = block()

// FILE: A3.kt

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@file:JvmPackageName("baz.foo.quux.bar")
@file:JvmName("Facade")
@file:JvmMultifileClass
package foo.bar

typealias S = String

fun f(): String = "O"

// FILE: B.kt

import foo.bar.*

fun box(): S = i { f() + g }
