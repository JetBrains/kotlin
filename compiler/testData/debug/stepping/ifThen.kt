// FILE: test.kt

fun foo() {
    if (flag) {
        return
    }
}

var flag = true

fun box() {
    foo()
    flag = false
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:12 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:13 box
// test.kt:14 box
// test.kt:4 foo
// test.kt:7 foo
// test.kt:15 box

// EXPECTATIONS JS_IR
// test.kt:12 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:13 box
// test.kt:14 box
// test.kt:4 foo
// test.kt:7 foo
// test.kt:15 box