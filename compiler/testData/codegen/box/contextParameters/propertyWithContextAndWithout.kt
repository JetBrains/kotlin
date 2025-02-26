// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// ISSUE: KT-74045: CONFLICTING_KLIB_SIGNATURES_ERROR
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

class A

context(a: A)
val b: String
    get() = "O"

val b: String
    get() = "K"

fun o() = with(A()) { b }
fun k() = b

fun box() = o() + k()
