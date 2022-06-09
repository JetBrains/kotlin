// FILE: test.kt

fun box() {
    {
        "OK"
    }()
}

// EXPECTATIONS
// test.kt:4 box
// EXPECTATIONS JVM
// test.kt:5 invoke
// EXPECTATIONS JVM_IR
// test.kt:5 box$lambda$0
// EXPECTATIONS
// test.kt:4 box
// test.kt:7 box
