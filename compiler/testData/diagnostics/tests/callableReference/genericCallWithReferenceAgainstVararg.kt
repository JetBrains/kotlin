// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo(vararg ints: Int) {}

typealias MyInt = Int
fun fooAlias(vararg ints: MyInt) {}

fun test(i: IntArray) {
    myLet(i, ::foo)
    myLet(::foo)
    myLet<Int>(<!TYPE_MISMATCH!>::foo<!>)
    myLet<IntArray>(::foo)
    myLetExplicit1(::foo)
    myLetExplicit2(::foo)

    myLet(i, ::fooAlias)
    myLet(::fooAlias)
    myLet<Int>(<!TYPE_MISMATCH!>::fooAlias<!>)
    myLet<IntArray>(::fooAlias)
    myLetExplicit1(::fooAlias)
    myLetExplicit2(::fooAlias)
}

fun <T> myLet(t: T, block: (T) -> Unit) {}
fun <T> myLet(block: (T) -> Unit) {}
fun myLetExplicit1(block: (Int) -> Unit) {}
fun myLetExplicit2(block: (IntArray) -> Unit) {}
