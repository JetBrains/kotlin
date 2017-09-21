package test

expect class C {
    fun foo()
    fun foo(n: Int)
    fun bar(n: Int)
}

fun test(c: C) {
    c.foo()
    c.foo(1)
    c.bar(1)
}