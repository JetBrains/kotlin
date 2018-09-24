package test

actual class Foo {
    actual fun <caret>bar() {}
}

// REF: [testModule_Common] (in test.Foo).bar()