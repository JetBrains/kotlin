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
// test.kt:4 box
// test.kt:16 box
// test.kt:5 box
// test.kt:20 fail
// test.kt:4 box
// test.kt:12 checkEquals
// test.kt:13 checkEquals
// test.kt:7 box
// test.kt:20 fail
// test.kt:8 box
// test.kt:16 box
// test.kt:7 box
// test.kt:12 checkEquals
// test.kt:13 checkEquals
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:20 fail
// test.kt:4 box
// test.kt:13 checkEquals
// test.kt:7 box
// test.kt:20 fail
// test.kt:7 box
// test.kt:13 checkEquals
// test.kt:9 box
