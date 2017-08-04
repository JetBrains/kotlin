package test

header class C {
    fun foo()
    fun <caret>foo(n: Int)
    fun bar(n: Int)
}

fun test(c: C) {
    c.foo()
    c.foo(1)
    c.bar(1)
}