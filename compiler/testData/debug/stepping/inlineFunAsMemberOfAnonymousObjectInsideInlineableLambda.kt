
// FILE: test.kt
inline fun foo(bar: () -> String) =
    bar()

fun box(): String {
    return foo(
        {
            val x = object {
                inline fun bar() = "OK"
            }
            x
                .bar()
        }
    )
}

// EXPECTATIONS JVM_IR
// test.kt:7 box
// test.kt:4 box
// test.kt:9 box
// test.kt:9 <init>
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box
// test.kt:10 box
// test.kt:13 box
// test.kt:4 box
// test.kt:7 box

// EXPECTATIONS NATIVE
// test.kt:7 box
// test.kt:4 box
// test.kt:9 box
// test.kt:9 box
// test.kt:12 box
// test.kt:13 box
// test.kt:10 box
// test.kt:13 box
// test.kt:4 box
// test.kt:15 box
// test.kt:16 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:9 <init>
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:7 $box (11)
// test.kt:4 $box (4)
// test.kt:9 $box (20)
// test.kt:11 $<no name provided>.<init> (13)
// test.kt:12 $box (12)
// test.kt:13 $box (17)
// test.kt:10 $box (35, 39)
// test.kt:13 $box (22)
// test.kt:4 $box (9)
// test.kt:7 $box (4)
