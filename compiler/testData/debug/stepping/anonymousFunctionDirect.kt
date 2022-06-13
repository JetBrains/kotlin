// FILE: test.kt

fun box() {
    {
        "OK"
    }()
}

// EXPECTATIONS
// EXPECTATIONS JVM_IR
// test.kt:5 box
// EXPECTATIONS JVM
// test.kt:4 box
// test.kt:5 invoke
// test.kt:4 box
// EXPECTATIONS
// test.kt:7 box
