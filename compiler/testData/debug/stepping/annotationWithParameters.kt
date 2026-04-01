
// FILE: test.kt

annotation class Anno(val a: Int, val b: Int = 2)

@Anno(4, 2)
fun foo(): String = "O"

@Anno(4)
fun bar(): String = "K"

fun box(): String =
    foo() +
            bar()

// EXPECTATIONS JVM_IR
// test.kt:13 box
// test.kt:7 foo
// test.kt:13 box
// test.kt:14 box
// test.kt:10 bar
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS NATIVE
// test.kt:13 box
// test.kt:7 foo
// test.kt:13 box
// test.kt:14 box
// test.kt:10 bar
// test.kt:14 box
// test.kt:13 box
// test.kt:13 box
// test.kt:14 box

// EXPECTATIONS JS_IR
// test.kt:14 box
// test.kt:7 foo
// test.kt:14 box
// test.kt:10 bar

// EXPECTATIONS WASM
// test.kt:13 $box (4)
// test.kt:7 $foo (20, 23)
// test.kt:14 $box (12)
// test.kt:10 $bar (20, 23)
// test.kt:13 $box (4)
// test.kt:14 $box (17)
