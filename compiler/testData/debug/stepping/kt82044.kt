// DONT_TARGET_EXACT_BACKEND: JS_IR
// ^^^ Test hangs while executing the debugger.



// MODULE: a
// FILE: a.kt

inline fun <T, R> T.myLet(block: (T) -> R): R {
    return block(this)
}

// MODULE: b(a)
// FILE: test.kt

fun box(): String {
    "O".myLet {
        val result = it + "K"
        return result
    }
}

// EXPECTATIONS JVM_IR
// test.kt:17 box
// a.kt:10 box
// test.kt:18 box
// test.kt:19 box

// EXPECTATIONS NATIVE
// test.kt:17 box
// a.kt:10 box
// test.kt:18 box
// test.kt:18 box
// test.kt:19 box
// test.kt:21 box

// EXPECTATIONS WASM
// test.kt:17 $box (4, 8)
// a.kt:10 $box (17, 11)
// test.kt:18 $box (21, 26, 21)
// test.kt:19 $box (15, 8)
