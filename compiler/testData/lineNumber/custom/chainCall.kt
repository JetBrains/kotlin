class A {
    fun foo() = this
    inline fun bar() = this
}

fun foo() {
    val a = A()
    a.foo()
        .foo()

    a.bar()
        .bar()
}

// 2 3 7 8 9 11 17 12 17 13
