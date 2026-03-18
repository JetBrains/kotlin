class A {
    operator fun plus(other: A): A = this
    operator fun minus(other: A): A = this
    operator fun times(other: A): A = this
    operator fun div(other: A): A = this
    operator fun rem(other: A): A = this
}

fun test() {
    val a = A()
    val b = A()
    a + b
    a - b
    a * b
    a / b
    a % b
}
