// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE
// !CHECK_TYPE

fun foo(d: dynamic) {
    val s: String = d
    val d1: dynamic = ""
    val d2: dynamic = null
}