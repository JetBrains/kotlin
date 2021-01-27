
// FILE: 1.kt

package test

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

// FILE: 2.kt

import test.*

fun box(): String {
    massert(true)
    massert(true) {
        "test"
    }

    return "OK"
}
