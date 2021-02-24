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

// LINENUMBERS
// test.kt:8 box
// test.kt:4 box
// test.kt:8 box
// test.kt:9 box
// test.kt:4 box
// test.kt:10 box
// test.kt:13 box
