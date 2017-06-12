// FLOW: OUT

fun foo(f: (Int) -> Int): Int {
    return f(1)
}

fun test() {
    fun bar(n: Int) = <caret>n
    val x = foo(::bar)
}