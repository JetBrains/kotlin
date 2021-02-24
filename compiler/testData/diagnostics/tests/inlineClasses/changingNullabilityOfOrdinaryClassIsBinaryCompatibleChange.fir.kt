// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class IC(val i: Int)

fun foo(a: Any, ic: IC) {}
fun foo(a: Any?, ic: IC) {}