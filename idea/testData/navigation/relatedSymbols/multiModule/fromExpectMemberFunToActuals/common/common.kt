package test

expect class Foo {
    fun <caret>bar()
}

// REF: [testModule_JVM] (in test.Foo).bar()
// REF: [testModule_JS] (in test.Foo).bar()