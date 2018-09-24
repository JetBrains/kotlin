package test

expect class Foo {
    class <caret>Bar
}

// REF: [testModule_JS] (in test.Foo).Bar
// REF: [testModule_JVM] (in test.Foo).Bar