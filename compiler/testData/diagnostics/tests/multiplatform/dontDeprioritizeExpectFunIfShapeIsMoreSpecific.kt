// ISSUE: KT-69069

// MODULE: m1-common
// FILE: common.kt

fun f(vararg elements: Int): Int = 0 // 1
expect fun f(element: Int): String // 2

fun test(): String = <!TYPE_MISMATCH!>f(42)<!> // Should be resolved to (2) because the shape is more specific if discard `expect` keyword

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun f(element: Int): String = "asdf"
