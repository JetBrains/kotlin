// IGNORE_BACKEND: JVM
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

// The JVM backend does not go back to line 4 and 7 for the
// addition. Instead it treats the addition of the evaluated
// arguments as being on line 5 and 8. That seems incorrect
// and the JVM_IR stepping is more correct.

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:12 box
// test.kt:4 box
// test.kt:5 box
// test.kt:16 fail
// test.kt:4 box
// test.kt:7 box
// test.kt:16 fail
// test.kt:7 box
// test.kt:8 box
// test.kt:12 box
// test.kt:7 box
// test.kt:9 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:4 box
// test.kt:16 fail
// test.kt:7 box
// test.kt:16 fail
// test.kt:12 box
// test.kt:7 box
// test.kt:9 box
