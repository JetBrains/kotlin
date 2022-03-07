// IGNORE_BACKEND_FIR: JVM_IR

// FILE: test.kt
fun box(): String {
    val
            o
            =
        "O"


    val k = "K"

    return o + k
}

// EXPECTATIONS

// test.kt:8 box
// test.kt:6 box
// test.kt:11 box
// test.kt:13 box