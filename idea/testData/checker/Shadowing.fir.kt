class A {
    operator fun component1() = 42
    operator fun component2() = 42
}

fun arrayA(): Array<A> = null!!

fun foo(a: A, c: Int) {
    val (a, b) = a
    val arr = arrayA()
    for ((c, d) in arr) {
    }
}

fun f(p: Int): Int {
    val p = 2
    val p = 3
    return p
}
