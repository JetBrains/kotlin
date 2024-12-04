// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Aaa
interface Bbb

fun f(a: Aaa) = a is Bbb
