class A(val n: Int) {
    fun invoke(i: Int): A = A(i)
}

fun test() {
    A(1).invoke(2)
    A(1)(2)
}