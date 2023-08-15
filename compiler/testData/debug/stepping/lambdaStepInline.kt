// IGNORE_BACKEND: WASM
// FILE: test.kt

inline fun foo(stringMaker: () -> String): String {
    return stringMaker()
}

fun box(): String {
    foo { "OK "}
    foo {
        "OK"
        // Comment
    }
    return "OK"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:5 box
// test.kt:9 box
// test.kt:5 box
// test.kt:10 box
// test.kt:5 box
// test.kt:11 box
// test.kt:5 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:14 box
