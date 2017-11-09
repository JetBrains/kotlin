// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(i: (Int) -> Unit) {}
fun foo2(i: (Int, Int) -> Unit) {}
fun foo3(i: (Pair) -> Unit) {}

fun bar(x: Int, y: Int) {
    foo1 { <!NAME_SHADOWING!>x<!> -> <!UNUSED_EXPRESSION!>x<!> }
    foo2 { <!NAME_SHADOWING!>x<!>: Int, <!NAME_SHADOWING!>y<!>: Int -> x + y }
    foo3 { (x, y) -> x + y }
}

data class Pair(val a: Int, val b: Int)