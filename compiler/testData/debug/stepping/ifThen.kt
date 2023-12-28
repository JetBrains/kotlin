
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

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:14 box
// test.kt:15 box
// test.kt:5 foo
// test.kt:8 foo
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:13 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:14 box
// test.kt:15 box
// test.kt:5 foo
// test.kt:8 foo
// test.kt:16 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:13 $box
// test.kt:5 $foo (8, 8)
// test.kt:6 $foo
// test.kt:14 $box (11, 4)
// test.kt:15 $box
// test.kt:8 $foo
// test.kt:16 $box
