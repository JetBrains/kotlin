
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:7 $box (4, 4)
// test.kt:15 $box (11, 11, 4, 11, 11, 4)
// test.kt:8 $box
// test.kt:19 $fail (11, 11, 11, 11, 4, 11, 11, 11, 11, 4)
// test.kt:4 $execute (38, 45, 38, 46, 38, 45, 38, 46)
// test.kt:10 $box (4, 4)
// test.kt:11 $box
// test.kt:12 $box
