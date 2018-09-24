package test

interface I {
    suspend fun /*rename*/foo(s: String)
}

fun test(i: I) {
    i.foo("test")
}