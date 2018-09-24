package test

interface I {
    suspend fun foo(s: String, n: Int): Int
}