// FILE: test.kt

var value = false

fun cond() = value

fun foo() {
    if (cond())
        cond()
    else
         false
}

fun box() {
    foo()
    value = true
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:15 box
// test.kt:8 foo
// test.kt:5 cond
// test.kt:8 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:16 box
// test.kt:17 box
// test.kt:8 foo
// test.kt:5 cond
// test.kt:8 foo
// test.kt:9 foo
// test.kt:5 cond
// test.kt:9 foo
// test.kt:12 foo
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:15 box
// test.kt:8 foo
// test.kt:5 cond
// test.kt:12 foo
// test.kt:16 box
// test.kt:17 box
// test.kt:8 foo
// test.kt:5 cond
// test.kt:9 foo
// test.kt:5 cond
// test.kt:12 foo
// test.kt:18 box