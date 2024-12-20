// FILE: test.kt

fun foo() {}

fun box() {
    0.apply {
        foo()
        this.apply {
            foo()
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:7 box
// test.kt:3 foo
// test.kt:8 box
// test.kt:9 box
// test.kt:3 foo
// test.kt:10 box
// test.kt:8 box
// test.kt:11 box
// test.kt:6 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:7 box
// test.kt:3 foo
// test.kt:9 box
// test.kt:3 foo
// test.kt:12 box

// EXPECTATIONS WASM
// test.kt:6 $box (4, 6)
// test.kt:7 $box (8)
// test.kt:3 $foo (12)
// test.kt:8 $box (8, 13)
// test.kt:9 $box (12)
// test.kt:3 $foo (12)
// test.kt:9 $box (12)
// test.kt:12 $box (1)
