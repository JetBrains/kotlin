
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

// EXPECTATIONS JVM_IR
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
// EXPECTATIONS FIR JS_IR
// test.kt:12 foo
// EXPECTATIONS JS_IR
// test.kt:13 foo
// test.kt:17 box
// test.kt:18 box
// test.kt:9 foo
// test.kt:6 cond
// test.kt:10 foo
// test.kt:6 cond
// EXPECTATIONS FIR JS_IR
// test.kt:12 foo
// EXPECTATIONS JS_IR
// test.kt:13 foo
// test.kt:19 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:16 $box
// test.kt:9 $foo (8, 8)
// test.kt:6 $cond (13, 18, 13, 18, 13, 18)
// test.kt:13 $foo (1, 1)
// test.kt:17 $box (12, 4)
// test.kt:18 $box
// test.kt:10 $foo (8, 8)
// test.kt:19 $box
