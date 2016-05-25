package test

class A {
    @JvmName("bar")
    fun first() = 1
}

fun test() {
    A().first()
}