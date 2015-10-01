class A(val n: Int) {
    operator fun minus(): A = this
}

fun test() {
    A(1).minus()
    -A(1)
}