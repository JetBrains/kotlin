// IGNORE_BACKEND: WASM
// FILE: test.kt

fun box(){
    checkEquals(test(),
                fail())

    checkEquals(fail(),
                test())
}

public fun checkEquals(p1: String, p2: String) {
    "check"
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    return "fail"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// test.kt:17 box
// test.kt:6 box
// test.kt:21 fail
// test.kt:5 box
// test.kt:13 checkEquals
// test.kt:14 checkEquals
// test.kt:8 box
// test.kt:21 fail
// test.kt:9 box
// test.kt:17 box
// test.kt:8 box
// test.kt:13 checkEquals
// test.kt:14 checkEquals
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:21 fail
// test.kt:5 box
// test.kt:14 checkEquals
// test.kt:8 box
// test.kt:21 fail
// test.kt:8 box
// test.kt:14 checkEquals
// test.kt:10 box
