package test

interface I {
    suspend fun bar(s: String)
}

fun test(i: I) {
    i.bar("test")
}