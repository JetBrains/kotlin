// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

class A {
    fun foo() {}
    val bar = 0
}

fun A.qux() {}

val test1 = A()::foo

val test2 = A()::bar

val test3 = A()::qux
