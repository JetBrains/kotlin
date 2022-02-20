// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt

expect fun ok(x: Int, y: String = "")

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual fun ok(x: Int, y: String) {}

fun ok(x: Int, y: Long = 1L) {}

fun test() {
    <!OVERLOAD_RESOLUTION_AMBIGUITY!>ok<!>(1)
}
