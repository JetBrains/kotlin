package test

actual class Foo {
    actual val <caret>bar: Int get() = 1
}

// REF: [common] (in test.Foo).bar