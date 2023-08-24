// IGNORE_BACKEND: WASM
// FILE: test.kt

var current = true

fun alternate(): Boolean {
    current = !current
    return current
}

fun foo() {
    while (true) {
        if (alternate()) {
            break
        }
    }
}

fun box() {
    foo()
}

// EXPECTATIONS JVM JVM_IR
// test.kt:20 box
// test.kt:12 foo
// test.kt:13 foo
// test.kt:7 alternate
// test.kt:8 alternate
// test.kt:13 foo
// test.kt:12 foo
// test.kt:13 foo
// test.kt:7 alternate
// test.kt:8 alternate
// test.kt:13 foo
// test.kt:14 foo
// test.kt:17 foo
// test.kt:21 box

// EXPECTATIONS JS_IR
// test.kt:20 box
// test.kt:7 alternate
// test.kt:8 alternate
// test.kt:7 alternate
// test.kt:8 alternate
// test.kt:17 foo
// test.kt:21 box
