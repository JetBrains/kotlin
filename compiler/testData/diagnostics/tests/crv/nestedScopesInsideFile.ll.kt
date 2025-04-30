// LL_FIR_DIVERGENCE
// ISSUE: KT-76776
// LL_FIR_DIVERGENCE
// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun localFun() {
    fun local(): Int = 123
    <!RETURN_VALUE_NOT_USED!>local()<!>     //unused
}

class A {
    fun foo(): Int = 123
    fun test() {
        foo()               //unused
    }
}
