class A(val n: Int) {
    fun foo(m: Int): A = A(n + m)
}

fun test() {
    A(1).foo(2)
    A(1) foo 2
    A(1).foo(2)

    var a = A(0)
    a = a.foo(1)
}