
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:5 $box (16, 4)
// test.kt:17 $box (11, 11, 4, 11, 11, 4)
// test.kt:6 $box
// test.kt:21 $fail (11, 11, 11, 11, 4, 11, 11, 11, 11, 4)
// test.kt:13 $checkEquals (4, 4, 4, 4, 4, 4, 4, 4, 4, 4)
// test.kt:14 $checkEquals (1, 1)
// test.kt:8 $box (16, 4)
// test.kt:9 $box
// test.kt:10 $box
