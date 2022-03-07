// WITH_STDLIB
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
fun box(): String {
    val p = Triple("X","O","K")

    val
            (
        _
            ,
        o
            ,
        k
    )
    =
        p

    return o + k
}

// EXPECTATIONS

// EXPECTATIONS JVM
// test.kt:6 box:

// test.kt:17 box: p:kotlin.Triple=kotlin.Triple
// test.kt:12 box: p:kotlin.Triple=kotlin.Triple
// test.kt:14 box: p:kotlin.Triple=kotlin.Triple

// test.kt:19 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JVM_IR
// test.kt:6 box:

// test.kt:17 box: p:kotlin.Triple=kotlin.Triple
// test.kt:12 box: p:kotlin.Triple=kotlin.Triple
// test.kt:14 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String

// test.kt:19 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String