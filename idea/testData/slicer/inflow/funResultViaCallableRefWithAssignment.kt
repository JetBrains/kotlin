// FLOW: IN

fun foo(f: (Int) -> Int): Int {
    return f(1)
}

fun test() {
    fun bar(n: Int) = n
    val f = ::bar
    val <caret>x = foo(f)
}