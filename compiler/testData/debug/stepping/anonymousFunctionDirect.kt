// FILE: test.kt

fun box() {
    {
        "OK"
    }()
}

// EXPECTATIONS JVM_IR
// test.kt:5 box
// EXPECTATIONS JVM
// test.kt:4 box
// test.kt:5 invoke
// test.kt:4 box
// EXPECTATIONS JVM JVM_IR
// test.kt:7 box

// EXPECTATIONS JS_IR
// test.kt:4 box
// test.kt:5 box$lambda
// test.kt:7 box