// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FILE: facade.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

package test

@JvmOverloads
fun foo(o: String = "O", k: String = "K"): String = o + k

// FILE: test.kt

fun box(): String =
    Class.forName("test.Facade").getMethod("foo").invoke(null) as String
