// !LANGUAGE: +InlineClasses

inline class IC(val i: Int)

fun foo(i: Int, ic: IC) {}
fun foo(i: Int?, ic: IC) {}