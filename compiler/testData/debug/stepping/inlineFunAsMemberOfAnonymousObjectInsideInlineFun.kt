
// FILE: test.kt
inline fun foo(): String {
    val x = object {
        inline fun bar() = "OK"
    }

    return x
        .bar()
}

fun box(): String {
    return foo()
}

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:4 box
// test.kt:4 <init>
// test.kt:4 box
// test.kt:8 box
// test.kt:9 box
// test.kt:5 box
// test.kt:8 box
// test.kt:13 box

// EXPECTATIONS NATIVE
// test.kt:13 box
// test.kt:4 box
// test.kt:4 box
// test.kt:8 box
// test.kt:9 box
// test.kt:5 box
// test.kt:9 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:4 <init>
// test.kt:13 box

// EXPECTATIONS WASM
// test.kt:13 $box (11)
// test.kt:4 $box (12)
// test.kt:6 $<no name provided>.<init> (5)
// test.kt:8 $box (11)
// test.kt:9 $box (9)
// test.kt:5 $box (27, 31)
// test.kt:8 $box (4)
// test.kt:13 $box (4)
