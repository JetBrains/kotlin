
// FILE: test.kt
inline fun foo() = "OK"

fun bar(x: String =
            foo()
): String =
    x

fun box(): String {
    return bar()
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:8 bar
// test.kt:11 box

// EXPECTATIONS NATIVE
// test.kt:11 box
// test.kt:5 bar$default
// test.kt:6 bar$default
// test.kt:3 bar$default
// test.kt:6 bar$default
// test.kt:8 bar$default
// test.kt:5 bar$default
// test.kt:5 bar
// test.kt:8 bar
// test.kt:5 bar$default
// test.kt:8 bar$default
// test.kt:11 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:3 bar
// test.kt:8 bar

// EXPECTATIONS WASM
// test.kt:11 $box (11)
// test.kt:6 $bar$default (12)
// test.kt:3 $bar$default (19, 23)
// test.kt:8 $bar (4, 5)
// test.kt:3 $bar$default (23)
// test.kt:11 $box (4)
