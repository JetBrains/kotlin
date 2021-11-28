// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

inline fun foo(l: () -> String): String = l()

inline fun foo2(l: () -> String): String = foo(l)


// FILE: 2.kt

fun box(): String = foo { "OK" }
