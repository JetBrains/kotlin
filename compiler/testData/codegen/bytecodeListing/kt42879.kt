inline class A(val value: Int)

fun interface I {
    fun compute(value: Int): A
}

fun f(i: I) {}

fun g() {
    f { it -> A(it) }
}
