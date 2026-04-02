
// FILE: test.kt

private val a = "OK"

internal inline fun foo(): String = a

// FILE: box.kt

fun box(): String {
    return foo()
}

// EXPECTATIONS JVM_IR

// EXPECTATIONS NATIVE
// box.kt:11 box
// test.kt:6 box
// test.kt:1 access$<get-a>$tTestKt
// test.kt:4 <get-a>
// test.kt:1 access$<get-a>$tTestKt
// test.kt:6 box
// box.kt:11 box
// box.kt:12 box

// EXPECTATIONS JS_IR
// box.kt:11 box
// test.kt:1 access$<get-a>$tTestKt

// EXPECTATIONS WASM
// box.kt:11 $box (11)
// test.kt:6 $box (36, 37)
// box.kt:11 $box (4)
