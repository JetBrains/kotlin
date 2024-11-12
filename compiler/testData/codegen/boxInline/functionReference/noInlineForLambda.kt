// ISSUE: KT-72884

// FILE: 1.kt
var result = "Fail"

fun bar() { result = "OK" }
inline fun foo(action: (() -> Unit) -> Unit) = action(::bar)

// FILE: 2.kt
fun box(): String {
    foo { it() }
    return result
}
