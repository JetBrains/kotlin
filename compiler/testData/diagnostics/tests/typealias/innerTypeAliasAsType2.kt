// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY -UNSUPPORTED_FEATURE

class C<T> {
    inner class D

    <!WRONG_MODIFIER_TARGET!>inner<!> typealias DA = D
    typealias SDA = C<Int>.D
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias TSDA = C<T>.D
    <!WRONG_MODIFIER_TARGET!>inner<!> typealias TC = C<T>
    typealias SSDA = C<*>.D
    typealias SSC = C<*>
}

fun test1(x: C<Int>.DA) = x
fun test2(x: C.SDA) = x
fun test3(x: C<Int>.TSDA) = x
fun test4(x: C<Int>.TC) = x

fun test5(x: C<*>.DA) = x
fun test6(x: C<*>.TSDA) = x
fun test7(x: C<*>.TC) = x

fun test8(x: C.SSDA) = x
fun test9(x: C.SSC) = x
