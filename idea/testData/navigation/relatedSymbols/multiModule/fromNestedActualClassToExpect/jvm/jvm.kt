package test

actual class Foo {
    actual class <caret>Bar
}

// REF: [testModule_Common] (in test.Foo).Bar