// MODULE: a
// FILE: a.kt

internal fun foo(s: String) = "FAIL"
internal fun foo(s: String, x: Any) = "OK"

// MODULE: b(a)
// FILE: box.kt

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
fun box() = foo("", Any())
