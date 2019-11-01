interface A {
    fun foo(b: Boolean = false): A
    fun foo(block: () -> Boolean): A
}

fun test(a: A) {
    a.foo { true }
}