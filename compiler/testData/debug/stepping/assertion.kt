// IGNORE_INLINER: IR

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

// EXPECTATIONS JVM_IR
// test.kt:26 box
// test.kt:17 box
// test.kt:18 box
// test.kt:5 getMASSERTIONS_ENABLED
// test.kt:18 box
// test.kt:19 box
// test.kt:23 box
// test.kt:27 box
// test.kt:8 box
// test.kt:5 getMASSERTIONS_ENABLED
// test.kt:8 box
// test.kt:9 box
// test.kt:14 box
// test.kt:31 box

// EXPECTATIONS JS_IR
// test.kt:17 box
// test.kt:18 box
// test.kt:8 box
// test.kt:31 box

// EXPECTATIONS WASM
// test.kt:26 $box (4, 12)
// test.kt:17 $box (57, 57, 57, 57)
// test.kt:18 $box
// test.kt:19 $box (13, 12)
// test.kt:27 $box (4, 12)
// test.kt:8 $box
// test.kt:9 $box (13, 12)
// test.kt:31 $box (11, 11, 11, 11, 4)
