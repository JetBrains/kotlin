package test

expect class C {
    fun foo()
    fun baz(n: Int)
    fun bar(n: Int)
}

fun test(c: C) {
    c.foo()
    c.baz(1)
    c.bar(1)
}