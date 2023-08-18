// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo1(i: (Int) -> Unit) {}
fun foo2(i: (Int, Int) -> Unit) {}
fun foo3(i: (Pair) -> Unit) {}

fun bar(x: Int, y: Int) {
    foo1 { x -> x }
    foo2 { x: Int, y: Int ->
        val x = x
    }
    foo3 { (x, y) ->
        val x = x
    }
}

data class Pair(val a: Int, val b: Int)