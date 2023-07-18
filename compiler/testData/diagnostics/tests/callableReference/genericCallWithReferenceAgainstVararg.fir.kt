// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(vararg ints: Int) {}
fun test(i: IntArray) {
    myLet(i, ::foo)
    myLet(::foo)
    myLet<Int>(::<!UNRESOLVED_REFERENCE!>foo<!>)
    myLet<IntArray>(::foo)
    myLetExplicit1(::foo)
    myLetExplicit2(::foo)
}

fun <T> myLet(t: T, block: (T) -> Unit) {}
fun <T> myLet(block: (T) -> Unit) {}
fun myLetExplicit1(block: (Int) -> Unit) {}
fun myLetExplicit2(block: (IntArray) -> Unit) {}
