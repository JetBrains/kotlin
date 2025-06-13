// ISSUE: KT-78168
// FILE: A.kt
private fun foo(x: String = "OK") = x
internal inline fun bar() = foo()

// FILE: B.kt

fun box() : String {
    return bar()
}