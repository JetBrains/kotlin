// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo(x: Any) {
    when (x) {
        is Float ->
            1
        is Double ->
            2
        else ->
            3
    }
}

fun box() {
    foo(1.2f)
    foo(1.2)
    foo(1)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:16 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:7 foo
// test.kt:13 foo
// test.kt:17 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:8 foo
// test.kt:9 foo
// test.kt:13 foo
// test.kt:18 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:19 box

// EXPECTATIONS JS_IR
// test.kt:16 box
// test.kt:13 foo
// test.kt:17 box
// test.kt:13 foo
// test.kt:18 box
// test.kt:13 foo
// test.kt:19 box
