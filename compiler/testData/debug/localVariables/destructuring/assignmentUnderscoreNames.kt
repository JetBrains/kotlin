// WITH_STDLIB

// FILE: test.kt
fun box(): String {
    val p = Triple("X","O","K")

    val ( _ , o, k ) = p

    return o + k
}

// EXPECTATIONS

// EXPECTATIONS JVM
// test.kt:5 box:
// test.kt:7 box: p:kotlin.Triple=kotlin.Triple
// test.kt:9 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:7 box: p:kotlin.Triple=kotlin.Triple
// test.kt:9 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String