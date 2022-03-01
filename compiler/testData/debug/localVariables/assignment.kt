

// FILE: test.kt
fun box(): String {
    val
            o
            =
        "O"


    val k = "K"

    return o + k
}

// A location for the expression being evaluated, and one for the store to the LV, but:
//    JVM: location for evaluating expression, location for the assigned variable
//    JVM_IR: location for evaluating expression, location for the val keyword

// EXPECTATIONS

// EXPECTATIONS JVM
// test.kt:8 box:
// test.kt:6 box:
// test.kt:11 box: o:java.lang.String="O":java.lang.String
// test.kt:13 box: o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JVM_IR
// test.kt:8 box:
// test.kt:5 box:
// test.kt:11 box: o:java.lang.String="O":java.lang.String
// test.kt:13 box: o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String