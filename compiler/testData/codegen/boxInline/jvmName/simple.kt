// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: 1.kt
package test

@JvmName("jvmName")
inline fun f(s: String = "OK"): String = s

// FILE: 2.kt

fun box(): String = test.f()
