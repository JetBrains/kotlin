package test

interface I {
    suspend fun <caret>foo(s: String)
}

// REF: [js] (in test.C).foo(String)
// REF: [jvm] (in test.C).foo(String)