class A {
    fun foo(s: String, i: Int) {}
}

fun usage(a: A) {
    a.foo("", 42)
    A().foo("", 42)
    a.foo("", 42)
}