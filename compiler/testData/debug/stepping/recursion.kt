// IGNORE_BACKEND: WASM
// FILE: test.kt
fun box() {
    val n = 3
    val k = foo(n)
}

fun foo(n :Int ) : Int {
    if (n == 1 || n == 0) {
        return 1
    }
    return foo(n-1) * n
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 foo
// test.kt:12 foo
// test.kt:9 foo
// test.kt:12 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:12 foo
// test.kt:5 box
// test.kt:6 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box
// test.kt:9 foo
// test.kt:12 foo
// test.kt:9 foo
// test.kt:12 foo
// test.kt:9 foo
// test.kt:10 foo
// test.kt:12 foo
// test.kt:12 foo
// test.kt:6 box