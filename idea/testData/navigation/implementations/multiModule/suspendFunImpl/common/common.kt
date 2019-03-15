package test

interface I {
    suspend fun <caret>foo(s: String)
}

// REF: [testModule_JS] (in test.C).foo(String)
// REF: [testModule_JVM] (in test.C).foo(String)