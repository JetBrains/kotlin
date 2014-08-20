class A(val n: Int) {
    fun foo(other: A): Int = n.compareTo(other.n)
}

fun test() {
    A(0) foo A(1)
    A(0).foo(A(1)) < 0
    A(0).foo(A(1)) <= 0
    A(0).foo(A(1)) > 0
    A(0).foo(A(1)) >= 0
}