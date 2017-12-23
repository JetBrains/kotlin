package test

interface I {
    suspend fun <caret>foo(s: String): Int
}