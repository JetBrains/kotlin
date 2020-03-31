// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
inline fun foo(stringMaker: () -> String = { "OK" }): String {
    return stringMaker()
}

inline fun foo2(stringMaker: () -> String = {
    "OK"
    // Comment
}): String {
    return stringMaker()
}

fun box(): String {
    foo()
    foo2()
    return "OK"
}

// The IR Backend does the following:
// test.kt:15
// test.kt:3
// test.kt:4
// test.kt:3
// test.kt:3 <---
// test.kt:16
// test.kt:7
// test.kt:11
// test.kt:8
// test.kt:7 <---
// test.kt:17

// LINENUMBERS
// test.kt:15
// test.kt:3
// test.kt:4
// test.kt:3
// test.kt:16
// test.kt:7
// test.kt:11
// test.kt:8
// test.kt:17