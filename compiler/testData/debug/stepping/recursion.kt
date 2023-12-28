
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
// test.kt:1 $box
// test.kt:4 $box
// test.kt:5 $box (16, 12, 4)
// test.kt:9 $foo (8, 13, 8, 18, 23, 18, 8, 13, 8, 18, 23, 18, 8, 13, 8)
// test.kt:12 $foo (15, 17, 15, 11, 15, 17, 15, 11, 22, 11, 4, 22, 11, 4)
// test.kt:10 $foo (15, 8)
// test.kt:6 $box
