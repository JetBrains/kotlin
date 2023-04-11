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
// test.kt:4 box
// EXPECTATIONS JVM_IR
// test.kt:5 box
// test.kt:7 box
// EXPECTATIONS JVM JVM_IR
// test.kt:10 box
// test.kt:12 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:12 box
