// WITH_STDLIB
// NO_CHECK_LAMBDA_INLINING

// FILE: 1.kt
var s = ""

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@kotlin.internal.InlineOnly
inline fun foo(s1: String, s2: String) = s1 + s2

// FILE: 2.kt
fun box(): String {
    val t = foo(s2 = { s += "O"; "K" }(), s1 = { s += "K"; "O" }())
    if (t != "OK") return "Failed: t=$t"
    if (s != "OK") return "Failed: s=$s"
    return "OK"
}
