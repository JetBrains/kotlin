
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

// EXPECTATIONS JVM_IR
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

// EXPECTATIONS WASM
// test.kt:19 $box (8, 4)
// test.kt:5 $foo (10, 10, 10, 10, 10, 10, 10)
// test.kt:6 $foo (8, 18, 14, 8, 8, 8, 8, 8, 8)
// test.kt:7 $foo (18, 14, 18, 14)
// test.kt:8 $foo (16, 16, 16, 16)
// test.kt:11 $foo (18, 4, 18, 18, 4, 4, 18, 18, 4, 18, 18, 4, 4, 4)
// test.kt:12 $foo (8, 8, 8, 8, 18, 14, 8, 8, 8)
// test.kt:14 $foo (16, 16, 16, 16, 16, 16, 16, 16)
// test.kt:16 $foo (1, 1, 1, 1, 1, 1, 1)
// test.kt:13 $foo (18, 14, 18, 14)
// test.kt:20 $box
