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
// test.kt:20 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:21 box
// test.kt:22 box
// test.kt:4 foo
// test.kt:7 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:23 box

// EXPECTATIONS JS_IR
// test.kt:20 box
// test.kt:4 foo
// test.kt:10 foo
// test.kt:11 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:21 box
// test.kt:22 box
// test.kt:4 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:10 foo
// test.kt:15 foo
// test.kt:23 box