class A(val n: Int) {
    fun get(i: Int): A = A(i)
}

fun test() {
    A(1).get(2)
    A(1)[2]
}