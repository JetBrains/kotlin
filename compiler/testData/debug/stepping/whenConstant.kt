// IGNORE_BACKEND: WASM
// FILE: test.kt

fun box() {
    when (1) {
        2 ->
            "2"
        3 ->
            "3"
        else ->
            "1"
    }
}

// JVM_IR and JVM backends have different heuristics for when to use a switch.

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box
// EXPECTATIONS JVM_IR
// test.kt:6 box
// test.kt:8 box
// EXPECTATIONS JVM JVM_IR
// test.kt:11 box
// test.kt:13 box

// EXPECTATIONS JS_IR
// test.kt:5 box
// test.kt:13 box
