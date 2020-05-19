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

// LINENUMBERS
// test.kt:22 box
// test.kt:12 test1
// test.kt:8 test1
// test.kt:9 test1
// test.kt:13 test1
// test.kt:14 test1
// test.kt:23 box
// test.kt:17 test2
// test.kt:4 normalFunction
// test.kt:5 normalFunction
// test.kt:18 test2
// test.kt:19 test2
// test.kt:24 box