// !LANGUAGE: +NewInference
// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(vararg ints: Int) {}
fun test(i: IntArray) {
    myLet(i, ::foo)
    myLet(::foo)
    <!INAPPLICABLE_CANDIDATE!>myLet<!><Int>(::foo)
    myLet<IntArray>(::foo)
    <!INAPPLICABLE_CANDIDATE!>myLetExplicit1<!>(::foo)
    myLetExplicit2(::foo)
}

fun <T> myLet(t: T, block: (T) -> Unit) {}
fun <T> myLet(block: (T) -> Unit) {}
fun myLetExplicit1(block: (Int) -> Unit) {}
fun myLetExplicit2(block: (IntArray) -> Unit) {}
