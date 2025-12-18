
// FILE: test.kt

private val a = "OK"

internal inline fun foo(): String = a

fun box(): String {
    return foo()
}

// EXPECTATIONS JVM_IR
// test.kt:9 box
// test.kt:6 box
// test.kt:9 box

// EXPECTATIONS NATIVE
// test.kt:9 box
// test.kt:6 box
// test.kt:1 access$<get-a>$tTestKt
// test.kt:1 access$<get-a>$tTestKt
// test.kt:6 box
// test.kt:9 box
// test.kt:10 box

// EXPECTATIONS JS_IR
// test.kt:9 box
// test.kt:1 access$<get-a>$tTestKt

// EXPECTATIONS WASM
// test.kt:9 $box (11)
// test.kt:6 $box (36, 37)
// test.kt:9 $box (4)
