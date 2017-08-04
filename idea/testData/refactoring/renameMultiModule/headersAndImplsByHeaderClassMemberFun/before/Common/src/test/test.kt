package test

header class C {
    fun /*rename*/foo()
    fun foo(n: Int)
    fun bar(n: Int)
}

fun test(c: C) {
    c.foo()
    c.foo(1)
    c.bar(1)
}