// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// WITH_STDLIB
// FULL_JDK
// JVM_TARGET: 1.8
// FILE: 1.kt

inline fun foo(crossinline init: (String) -> String): String =
    listOf("OK").stream().map { init(it) }.findFirst().get()

inline fun bar(crossinline init: (String) -> String): String =
    foo { foo(init) }

// FILE: 2.kt

fun box(): String = bar { it }
