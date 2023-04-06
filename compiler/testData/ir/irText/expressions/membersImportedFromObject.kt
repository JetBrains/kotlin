// FIR_IDENTICAL

// MUTE_SIGNATURE_COMPARISON_K2: JS_IR
// MUTE_SIGNATURE_COMPARISON_K2: NATIVE
// ^ KT-57818

import A.foo
import A.bar
import A.fooExt
import A.barExt

object A {
    fun foo() = 1
    fun Int.fooExt() = 2
    val bar = 42
    val Int.barExt get() = 43
}

val test1 = foo()
val test2 = bar
val test3 = 1.fooExt()
val test4 = 1.barExt
