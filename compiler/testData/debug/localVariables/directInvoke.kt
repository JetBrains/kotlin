// FILE: test.kt
fun box() {
    { a: String, b: String->
        a + b
    }("O", "K")
}

// EXPECTATIONS JVM_IR
// test.kt:4 box:
// test.kt:6 box:

// EXPECTATIONS JS_IR
// test.kt:3 box:
// test.kt:4 box$lambda: a="O":kotlin.String, b="K":kotlin.String
// test.kt:6 box:
