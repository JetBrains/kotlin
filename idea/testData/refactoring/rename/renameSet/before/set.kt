class A(val n: Int) {
    fun set(i: Int, a: A) {}
}

fun test() {
    var a = A(1)
    a.set(2, A(2))
    a[2] = A(2)
    a[2]
}