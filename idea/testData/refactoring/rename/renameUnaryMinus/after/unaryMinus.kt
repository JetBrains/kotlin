class A(val n: Int) {
    fun foo(): A = this
}

fun test() {
    A(1).foo()
    A(1).foo()
}