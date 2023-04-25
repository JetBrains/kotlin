// WITH_STDLIB

// FILE: test.kt
fun box(): String {
    val p = "O" to "K"

    val ( o , k ) = p

    return o + k
}

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box:
// test.kt:7 box: p:kotlin.Pair=kotlin.Pair
// test.kt:9 box: p:kotlin.Pair=kotlin.Pair, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:7 box: p=kotlin.Pair
// test.kt:7 box: p=kotlin.Pair, o="O":kotlin.String
// test.kt:9 box: p=kotlin.Pair, o="O":kotlin.String, k="K":kotlin.String
