// FILE: test.kt

// Comment before
fun foo(i: Int = 1): Int {
    return i
}

fun box() {
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:9 box
// test.kt:5 foo
// test.kt:9 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:10 box
