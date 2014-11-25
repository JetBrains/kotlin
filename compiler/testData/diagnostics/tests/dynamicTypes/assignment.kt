// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE

// MODULE[js]: m1
// FILE: k.kt

fun foo(d: dynamic) {
    val s: String = d
    val d1: dynamic = ""
    val d2: dynamic = null
}