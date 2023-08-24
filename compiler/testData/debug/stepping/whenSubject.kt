// IGNORE_BACKEND: WASM
// FILE: test.kt

fun foo(x: Int) {
    when (x) {
        21 -> foo(42)
        42 -> foo(63)
        else -> 1
    }
    
    val t = when (x) {
        21 -> foo(42)
        42 -> foo(63)
        else -> 1
    }
}

fun box() {
    foo(21)
}

// EXPECTATIONS JVM JVM_IR
// test.kt:19 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:7 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:6 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:7 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:5 foo
// test.kt:8 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:13 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:12 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:20 box

// EXPECTATIONS JS_IR
// test.kt:19 box
// test.kt:5 foo
// test.kt:6 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:12 foo
// test.kt:5 foo
// test.kt:7 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:13 foo
// test.kt:5 foo
// test.kt:11 foo
// test.kt:14 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:11 foo
// test.kt:16 foo
// test.kt:20 box
