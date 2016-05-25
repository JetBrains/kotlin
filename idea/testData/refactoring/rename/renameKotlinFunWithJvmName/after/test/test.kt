package test

class A {
    @JvmName("foo")
    fun second() = 1
}

fun test() {
    A().second()
}