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
// test.kt:6 box
// test.kt:14 box
// test.kt:7 box
// test.kt:18 fail
// test.kt:6 box
// test.kt:3 execute
// test.kt:6 box
// test.kt:9 box
// test.kt:18 fail
// test.kt:10 box
// test.kt:14 box
// test.kt:9 box
// test.kt:3 execute
// test.kt:9 box
// test.kt:11 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:18 fail
// test.kt:6 box
// test.kt:3 execute
// test.kt:9 box
// test.kt:18 fail
// test.kt:9 box
// test.kt:3 execute
// test.kt:11 box
