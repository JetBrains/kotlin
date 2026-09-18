// FILE: test.kt

fun test(x: Int, y: Int): Int {
    fun foo(
            z: Int) =
                x + z
    return foo(y)
}

fun box() {
    test(109, 136)
}

// EXPECTATIONS JVM_IR
// test.kt:11 box
// test.kt:7 test
// test.kt:6 test$foo
// test.kt:7 test
// test.kt:11 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:11 box
// test.kt:7 test
// test.kt:6 test$foo
// test.kt:12 box

// EXPECTATIONS WASM
// test.kt:11 $box (9, 14, 4)
// test.kt:7 $test (11, 15, 11)
// test.kt:6 $foo (16, 20, 16, 21)
// test.kt:7 $test (4)
// test.kt:11 $box (4)
// test.kt:12 $box (1)

// EXPECTATIONS NATIVE
// test.kt:11 box
// test.kt:3 test
// test.kt:7 test
// test.kt:4 test$foo
// test.kt:6 test$foo
// test.kt:7 test
// test.kt:8 test
// test.kt:12 box
