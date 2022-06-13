// FILE: test.kt
fun box() {
    { a: String, b: String->
        a + b
    }("O", "K")
}

// EXPECTATIONS
// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:4 box: a:java.lang.String="O":java.lang.String, b:java.lang.String="K":java.lang.String
// EXPECTATIONS JVM
// test.kt:3 box:
// test.kt:5 box:
// test.kt:4 invoke: a:java.lang.String="O":java.lang.String, b:java.lang.String="K":java.lang.String
// test.kt:5 box:
// EXPECTATIONS
// test.kt:6 box: