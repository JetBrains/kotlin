// IGNORE_BACKEND_K1: JVM_IR
// FILE: test.kt

inline fun foo(inlined: () -> String, noinline notInlined: () -> String): String =
    inlined() +
            notInlined()

fun box(): String =
    foo({ "O" },
        { "K" })

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:5 box
// test.kt:9 box
// test.kt:5 box
// test.kt:6 box
// test.kt:10 box$lambda$1
// test.kt:6 box
// test.kt:5 box
// test.kt:6 box
// test.kt:10 box

// EXPECTATIONS NATIVE
// test.kt:10 box
// test.kt:9 box
// test.kt:9 box
// test.kt:6 box
// test.kt:6 box
// test.kt:5 box
// test.kt:5 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:10 box
// test.kt:10 box$lambda

// EXPECTATIONS WASM
// test.kt:9 $box (4)
// test.kt:5 $box (4)
// test.kt:9 $box (10, 13)
// test.kt:6 $box (12)
// test.kt:10 $box$lambda.invoke (10, 13)
// test.kt:6 $box (12)
// test.kt:5 $box (4)
// test.kt:6 $box (24)
// test.kt:10 $box (16)
