
// FILE: test.kt
inline fun foo1(): String = "O"

fun foo2(): String = "K"

inline fun bar(baz: () -> String) =
    baz()

fun box(): String {
    return bar(
        { foo1() }
    ) + bar(
        { foo2() }
    )
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:8 box
// test.kt:12 box
// test.kt:3 box
// test.kt:12 box
// test.kt:8 box
// test.kt:11 box
// test.kt:13 box
// test.kt:8 box
// test.kt:14 box
// test.kt:5 foo2
// test.kt:14 box
// test.kt:8 box
// test.kt:11 box

// EXPECTATIONS NATIVE
// test.kt:11 box
// test.kt:8 box
// test.kt:12 box
// test.kt:3 box
// test.kt:12 box
// test.kt:8 box
// test.kt:13 box
// test.kt:8 box
// test.kt:14 box
// test.kt:5 foo2
// test.kt:14 box
// test.kt:8 box
// test.kt:11 box
// test.kt:11 box
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:3 box
// test.kt:11 box
// test.kt:5 foo2

// EXPECTATIONS WASM
// test.kt:11 $box (11)
// test.kt:8 $box (4)
// test.kt:12 $box (10)
// test.kt:3 $box (28, 31)
// test.kt:12 $box (16)
// test.kt:8 $box (9)
// test.kt:13 $box (8)
// test.kt:8 $box (4)
// test.kt:14 $box (10)
// test.kt:5 $foo2 (21, 24)
// test.kt:14 $box (16)
// test.kt:8 $box (9)
// test.kt:11 $box (11, 4)
