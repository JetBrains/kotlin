class A(val n: Int) {
    fun plus(m: Int): A = A(n + m)
}

fun test() {
    A(1) + 2
    A(1) plus 2
    A(1).plus(2)

    var a = A(0)
    a += 1
}