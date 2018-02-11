package test

actual class Foo {
    actual class <caret>Bar
}

// REF: [common] (in test.Foo).Bar