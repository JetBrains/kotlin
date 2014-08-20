class A(val n: Int) {
    fun foo(i: Int, a: A) {}
}

fun test() {
    var a = A(1)
    a.foo(2, A(2))
    a.foo(2, A(2))
    a[2]
}