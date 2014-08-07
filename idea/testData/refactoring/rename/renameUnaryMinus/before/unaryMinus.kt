class A(val n: Int) {
    fun minus(): A = this
}

fun test() {
    A(1).minus()
    -A(1)
}