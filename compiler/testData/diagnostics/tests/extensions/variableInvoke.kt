class A(foo: Int.() -> Unit) {
    init {
        4.foo()
    }
}

fun test(foo: Int.() -> Unit) {
    4.foo()
}