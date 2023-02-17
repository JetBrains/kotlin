// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
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

// EXPECTATIONS JVM JVM_IR
// test.kt:16 box
// test.kt:4 box
// test.kt:5 box
// test.kt:4 box
// test.kt:5 box
// test.kt:17 box
// test.kt:8 box
// test.kt:12 box
// test.kt:9 box
// test.kt:12 box
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 box
// test.kt:12 box
// test.kt:18 box
