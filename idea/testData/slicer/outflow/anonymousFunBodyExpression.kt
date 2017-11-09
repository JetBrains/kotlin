// FLOW: OUT

fun foo(f: (Int) -> Int): Int {
    return f(1)
}

fun test() {
    val x = foo(fun(n: Int) = <caret>n)
}