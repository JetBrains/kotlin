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
// test.kt:24 box
// test.kt:15 box
// test.kt:16 box
// test.kt:3 getMASSERTIONS_ENABLED
// test.kt:16 box
// test.kt:17 box
// test.kt:21 box
// test.kt:25 box
// test.kt:6 box
// test.kt:3 getMASSERTIONS_ENABLED
// test.kt:6 box
// test.kt:7 box
// test.kt:12 box
// test.kt:29 box
