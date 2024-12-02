
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:3 $box (10)
// test.kt:4 $box (12)
// test.kt:5 $box (16, 12)
// test.kt:8 $foo (23)
// test.kt:9 $foo (8, 13, 18, 23)
// test.kt:10 $foo (8, 15)
// test.kt:12 $foo (15, 17, 11, 22, 4)
// test.kt:6 $box (1)
