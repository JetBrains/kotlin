package test

actual class Foo {
    actual fun <caret>bar() {}
}

// REF: [common] (in test.Foo).bar()