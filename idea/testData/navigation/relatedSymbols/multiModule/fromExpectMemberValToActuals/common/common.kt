package test

expect class Foo {
    val <caret>bar: Int
}

// REF: [testModule_JVM] (in test.Foo).bar
// REF: [testModule_JS] (in test.Foo).bar