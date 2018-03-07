package test

actual class Expected {
    actual fun foo() = 42

    actual val <caret>bar = "Hello"
}

// REF: [common] (in test.Expected).bar

