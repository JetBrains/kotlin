// FLOW: OUT

fun foo(f: (Int) -> Int): Int {
    val x = f
    return x(1)
}

fun test() {
    val y = foo { <caret>it }
}