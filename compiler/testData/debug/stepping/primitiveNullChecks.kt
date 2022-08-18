// FILE: test.kt

fun box(): String {
    42!!
    42.toLong()!!
    return "OK"!!
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:5 box
// test.kt:6 box
