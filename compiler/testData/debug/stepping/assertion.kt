// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
public val MASSERTIONS_ENABLED: Boolean = true

public inline fun massert(value: Boolean, lazyMessage: () -> String) {
    if (MASSERTIONS_ENABLED) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}


public inline fun massert(value: Boolean, message: Any = "Assertion failed") {
    if (MASSERTIONS_ENABLED) {
        if (!value) {
            throw AssertionError(message)
        }
    }
}

fun box(): String {
    massert(true)
    massert(true) {
        "test"
    }

    return "OK"
}

// LINENUMBERS
// test.kt:24
// test.kt:15
// test.kt:16
// test.kt:3
// test.kt:16
// test.kt:17
// test.kt:21
// test.kt:25
// test.kt:6
// test.kt:3
// test.kt:6
// test.kt:7
// test.kt:12
// test.kt:29