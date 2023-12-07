// IGNORE_BACKEND: WASM

// FILE: test.kt
fun box(){
    test() +
            fail()

    fail() +
                test()
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    return "fail"
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:13 box
// test.kt:5 box
// test.kt:6 box
// test.kt:17 fail
// test.kt:5 box
// test.kt:8 box
// test.kt:17 fail
// test.kt:8 box
// test.kt:9 box
// test.kt:13 box
// test.kt:8 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:17 fail
// test.kt:8 box
// test.kt:17 fail
// test.kt:8 box
// test.kt:10 box
