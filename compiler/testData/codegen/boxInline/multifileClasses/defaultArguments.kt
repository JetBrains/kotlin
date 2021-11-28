// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: 1.kt

@file:JvmName("Facade")
@file:JvmMultifileClass

inline fun foo(o: String, k: String = "K", body: (String) -> String): String =
    o + bar(body(k))

fun bar(x: String): String = x

// FILE: 2.kt

fun box(): String = foo("O") { it }
