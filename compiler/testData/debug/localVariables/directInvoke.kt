// FILE: test.kt
fun box() {
    { a: String, b: String->
        a + b
    }("O", "K")
}

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:4 box: a:java.lang.String="O":java.lang.String, b:java.lang.String="K":java.lang.String
// test.kt:6 box:

// EXPECTATIONS JS_IR
// test.kt:3 box:
// test.kt:4 box$lambda: a="O":kotlin.String, b="K":kotlin.String
// test.kt:6 box:
