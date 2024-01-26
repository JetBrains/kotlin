// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// FILE: test.kt
@JvmName("jvmName")
inline fun f(s: String = "OK"): String = s

fun box(): String = f()

fun main() {
    box()
}

// EXPECTATIONS JVM_IR
// test.kt:8 box
// test.kt:6 box
// test.kt:8 box
