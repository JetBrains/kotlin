
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:9 $box (4, 10, 10, 15)
// test.kt:5 $box (11, 4, 11, 4)
// test.kt:10 $box
// test.kt:11 $box (8, 8, 12)
// test.kt:14 $box (11, 11, 11, 11, 4)
