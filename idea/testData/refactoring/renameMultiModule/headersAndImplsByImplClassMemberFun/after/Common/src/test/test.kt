package test

expect class C {
    fun baz()
    fun baz(n: Int)
    fun bar(n: Int)
}

fun test(c: C) {
    c.baz()
    c.baz(1)
    c.bar(1)
}