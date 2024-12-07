
// FILE: test.kt

fun normalFunction() {
    "OK"
}

inline fun inlineFunction() {
    "OK"
}

fun test1() {
    inlineFunction()
    "OK"
}

fun test2() {
    normalFunction()
    "OK"
}

fun box() {
    test1()
    test2()
}

// EXPECTATIONS JVM_IR
// test.kt:23 box
// test.kt:13 test1
// test.kt:9 test1
// test.kt:10 test1
// test.kt:14 test1
// test.kt:15 test1
// test.kt:24 box
// test.kt:18 test2
// test.kt:5 normalFunction
// test.kt:6 normalFunction
// test.kt:19 test2
// test.kt:20 test2
// test.kt:25 box

// EXPECTATIONS JS_IR
// test.kt:23 box
// test.kt:15 test1
// test.kt:24 box
// test.kt:18 test2
// test.kt:6 normalFunction
// test.kt:20 test2
// test.kt:25 box

// EXPECTATIONS WASM
// test.kt:23 $box
// test.kt:13 $test1
// test.kt:9 $test1 (4, 4, 4)
// test.kt:14 $test1 (4, 4, 4, 4, 4)
// test.kt:15 $test1
// test.kt:24 $box
// test.kt:18 $test2
// test.kt:5 $normalFunction (4, 4, 4, 4, 4)
// test.kt:6 $normalFunction
// test.kt:19 $test2 (4, 4, 4, 4, 4)
// test.kt:20 $test2
// test.kt:25 $box
