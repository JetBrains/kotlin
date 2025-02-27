// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// ISSUE: KT-74045: CONFLICTING_KLIB_SIGNATURES_ERROR

class A

context(a: A)
val b: String
    get() = "O"

val b: String
    get() = "K"

fun o() = with(A()) { b }
fun k() = b

fun box() = o() + k()
