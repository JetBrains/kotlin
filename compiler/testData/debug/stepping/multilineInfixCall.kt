// FILE: test.kt

fun box() {
    1 foo
         1
}

infix fun Int.foo(i: Int) {
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:4 box
// test.kt:9 foo
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:9 foo
// test.kt:6 box