
// FILE: test.kt

fun foo(x: Int) {
    if (x > 0) {
        "OK"
    }

    if (x > 0) else {
        "OK"
    }

    if (x > 0) {
        "OK"
    } else {
        "ALSO OK"
    }
}

fun box() {
    foo(1)
    foo(0)
}

// EXPECTATIONS JVM_IR
// test.kt:21 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:9 foo
// test.kt:13 foo
// test.kt:14 foo
// test.kt:18 foo
// test.kt:22 box
// test.kt:5 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:13 foo
// test.kt:16 foo
// test.kt:18 foo
// test.kt:23 box

// EXPECTATIONS JS_IR
// test.kt:21 box
// test.kt:18 foo
// test.kt:22 box
// test.kt:18 foo
// test.kt:23 box

// EXPECTATIONS WASM
// test.kt:21 $box (8, 4)
// test.kt:5 $foo (8, 12, 8, 8, 12, 8)
// test.kt:6 $foo (8, 8, 8, 8)
// test.kt:9 $foo (8, 12, 8, 8, 12, 8)
// test.kt:13 $foo (8, 12, 8, 8, 12, 8)
// test.kt:14 $foo (8, 8)
// test.kt:18 $foo (1, 1)
// test.kt:22 $box (8, 4)
// test.kt:10 $foo (8, 8, 8)
// test.kt:16 $foo (8, 8, 8, 8, 8)
// test.kt:23 $box
