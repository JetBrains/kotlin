// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo() {
    if (flag) {
        "OK"
    } else {
        "OK"
    }
    
    val b = if (flag) {
        "OK"
    } else {
        "OK"
    }
}

var flag = true

fun box() {
    foo()
    flag = false
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:21 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:22 box
// test.kt:23 box
// test.kt:5 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:24 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:5 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:22 box
// test.kt:23 box
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:24 box