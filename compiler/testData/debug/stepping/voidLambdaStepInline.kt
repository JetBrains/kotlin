// FILE: test.kt

fun box(): String {
    run { "O" + "K" }
    run {
        "O" + "K"
    }
    return "OK"
}

// EXPECTATIONS JVM JVM_IR
// test.kt:4 box
// fake.kt:1 box
// test.kt:4 box
// test.kt:5 box
// test.kt:6 box
// test.kt:5 box
// test.kt:8 box

// EXPECTATIONS JS_IR
// test.kt:8 box
