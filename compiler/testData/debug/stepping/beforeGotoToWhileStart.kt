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
// test.kt:19 box
// test.kt:11 foo
// test.kt:12 foo
// test.kt:6 alternate
// test.kt:7 alternate
// test.kt:12 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:6 alternate
// test.kt:7 alternate
// test.kt:12 foo
// test.kt:13 foo
// test.kt:16 foo
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:19 box
// test.kt:6 alternate
// test.kt:7 alternate
// test.kt:6 alternate
// test.kt:7 alternate
// test.kt:16 foo
// test.kt:20 box
