// IGNORE_BACKEND: WASM
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
// test.kt:16 box
// test.kt:9 foo
// test.kt:6 cond
// test.kt:9 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:17 box
// test.kt:18 box
// test.kt:9 foo
// test.kt:6 cond
// test.kt:9 foo
// test.kt:10 foo
// test.kt:6 cond
// test.kt:10 foo
// test.kt:13 foo
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// test.kt:9 foo
// test.kt:6 cond
// test.kt:13 foo
// test.kt:17 box
// test.kt:18 box
// test.kt:9 foo
// test.kt:6 cond
// test.kt:10 foo
// test.kt:6 cond
// test.kt:13 foo
// test.kt:19 box