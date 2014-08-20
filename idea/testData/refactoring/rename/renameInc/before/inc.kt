class A(val n: Int) {
    fun inc(): A = A(n + 1)
}

fun test() {
    var a = A(1)
    a.inc()
    ++a
    a++
}