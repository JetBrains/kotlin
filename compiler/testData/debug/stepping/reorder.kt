
// FILE: test.kt
fun foo(a: Int, b: Int) {}

fun box() {
    foo(
        b = if (true) {
            1
        } else {
            2
        },
        a = if (true) {
            3
        } else {
            4
        }
    )
}


// EXPECTATIONS JVM_IR
// test.kt:7 box
// test.kt:8 box
// test.kt:7 box
// test.kt:12 box
// test.kt:13 box
// test.kt:7 box
// test.kt:6 box
// test.kt:3 foo
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// test.kt:13 box
// test.kt:6 box
// test.kt:3 foo
// test.kt:18 box

// EXPECTATIONS WASM
// test.kt:8 $box (12)
// test.kt:7 $box (12)
// test.kt:13 $box (12)
// test.kt:12 $box (12)
// test.kt:7 $box (12)
// test.kt:6 $box (4)
// test.kt:3 $foo (26)
// test.kt:18 $box (1)
