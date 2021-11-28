// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: facade.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

package test

@JvmOverloads
fun foo(o: String = "O", k: String = "K"): String = o + k

// FILE: test.kt

fun box(): String =
    Class.forName("test.Facade").getMethod("foo").invoke(null) as String
