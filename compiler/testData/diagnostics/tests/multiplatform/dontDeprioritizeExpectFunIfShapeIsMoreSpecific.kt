// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-69069

// MODULE: m1-common
// FILE: common.kt

<!CONFLICTING_OVERLOADS!>fun f(vararg elements: Int): Int<!> = 0 // 1
<!CONFLICTING_OVERLOADS!>expect fun f(element: Int): String<!> // 2

<!CONFLICTING_OVERLOADS!>fun test(): String<!> = <!OVERLOAD_RESOLUTION_AMBIGUITY!>f<!>(42) // Should be resolved to (2) because the shape is more specific if discard `expect` keyword

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun f(element: Int): String = "asdf"
