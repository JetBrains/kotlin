package test

actual class Expected {
    actual fun <caret>foo() = 42

    actual val bar = "Hello"
}

// REF: [common] (in test.Expected).foo()

