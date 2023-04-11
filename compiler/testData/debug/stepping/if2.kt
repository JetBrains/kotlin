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

// EXPECTATIONS JVM JVM_IR
// test.kt:20 box
// test.kt:4 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:17 foo
// test.kt:21 box
// test.kt:4 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:12 foo
// test.kt:15 foo
// test.kt:17 foo
// test.kt:22 box

// EXPECTATIONS JS_IR
// test.kt:20 box
// test.kt:17 foo
// test.kt:21 box
// test.kt:17 foo
// test.kt:22 box
