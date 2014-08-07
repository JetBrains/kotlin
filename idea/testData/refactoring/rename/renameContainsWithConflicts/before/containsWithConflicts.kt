class A(val n: Int) {
    fun contains(k: Int): Boolean = k <= n
}

fun test() {
    A(2) contains 1
    1 in A(2)
    1 !in A(2)
    when (1) {
        in A(2) -> {}
        !in A(2) -> {}
    }
}