// FLOW: IN

fun foo(f: (Int) -> Int): Int {
    val x = f
    return x(1)
}

fun test() {
    val <caret>y = foo { it }
}