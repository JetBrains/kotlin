
// FILE: test.kt

fun box() {
    1 foo
         1
}

infix fun Int.foo(i: Int) {
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:6 box
// test.kt:5 box
// test.kt:10 foo
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:10 foo
// test.kt:7 box

// EXPECTATIONS WASM
// test.kt:1 $box
// test.kt:5 $box (4, 4)
// test.kt:6 $box
// test.kt:10 $foo
// test.kt:7 $box
