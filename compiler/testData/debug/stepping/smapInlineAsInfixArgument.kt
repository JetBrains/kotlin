// IGNORE_BACKEND: WASM
// FILE: test.kt

infix fun String.execute(p: String) = this + p

fun box(){
    test() execute
            fail()

    fail() execute
            test()
}

inline fun test() : String {
    return "123"
}

fun fail() : String {
    return "fail"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:7 box
// test.kt:15 box
// test.kt:8 box
// test.kt:19 fail
// test.kt:7 box
// test.kt:4 execute
// test.kt:7 box
// test.kt:10 box
// test.kt:19 fail
// test.kt:11 box
// test.kt:15 box
// test.kt:10 box
// test.kt:4 execute
// test.kt:10 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:19 fail
// test.kt:7 box
// test.kt:4 execute
// test.kt:10 box
// test.kt:19 fail
// test.kt:10 box
// test.kt:4 execute
// test.kt:12 box
