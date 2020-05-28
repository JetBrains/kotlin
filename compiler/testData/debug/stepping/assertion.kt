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

// JVM_IR steps on line 15 both on the way in and on the way out
// of the massert method. This is consistent with what would
// happen if massert was not inline and we used force-step-into
// to step through the $default method. JVM only hits line 15 on
// the way in.

// LINENUMBERS
// test.kt:24 box
// test.kt:15 box
// test.kt:16 box
// test.kt:3 getMASSERTIONS_ENABLED
// test.kt:16 box
// test.kt:17 box
// test.kt:21 box
// LINENUMBERS JVM_IR
// test.kt:15 box
// LINENUMBERS
// test.kt:25 box
// test.kt:6 box
// test.kt:3 getMASSERTIONS_ENABLED
// test.kt:6 box
// test.kt:7 box
// test.kt:12 box
// test.kt:29 box
