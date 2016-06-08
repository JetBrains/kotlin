package test

class A {
    @JvmName("foo")
    fun first() = 1
}

fun test() {
    A().first()
}