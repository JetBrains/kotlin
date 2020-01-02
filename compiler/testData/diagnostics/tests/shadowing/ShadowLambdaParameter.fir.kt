// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(i: (Int) -> Unit) {}
fun foo2(i: (Int, Int) -> Unit) {}
fun foo3(i: (Pair) -> Unit) {}

fun bar(x: Int, y: Int) {
    foo1 { x -> x }
    foo2 { x: Int, y: Int -> x + y }
    foo3 { (x, y) -> x + y }
}

data class Pair(val a: Int, val b: Int)