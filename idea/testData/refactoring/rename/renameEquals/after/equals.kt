class A(val n: Int) {
    override fun foo(other: Any?): Boolean = other is A && other.n == n
}

fun test() {
    A(0).foo(A(1))
    !A(0).foo(A(1))
    A(0) foo A(1)
    A(0) === A(1)
    A(0) !== A(1)
}