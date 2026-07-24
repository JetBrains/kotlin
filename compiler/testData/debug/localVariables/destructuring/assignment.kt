// WITH_STDLIB

// FILE: test.kt
fun box(): String {
    val p = "O" to "K"

    val ( o , k ) = p

    return o + k
}

// EXPECTATIONS JVM_IR
// test.kt:5 box:
// test.kt:7 box: p:kotlin.Pair=kotlin.Pair
// test.kt:9 box: p:kotlin.Pair=kotlin.Pair, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:7 box: p=kotlin.Pair
// test.kt:7 box: p=kotlin.Pair, o="O":kotlin.String
// test.kt:9 box: p=kotlin.Pair, o="O":kotlin.String, k="K":kotlin.String

// EXPECTATIONS WASM
// test.kt:5 $box: $p:(ref null $kotlin.Pair)=null, $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (12, 12, 12, 19, 19, 19, 12)
// test.kt:7 $box: $p:(ref $kotlin.Pair)=(ref $kotlin.Pair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref null $kotlin.String)=null (20, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14)
// test.kt:9 $box: $p:(ref $kotlin.Pair)=(ref $kotlin.Pair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref $kotlin.String)=(ref $kotlin.String) (11, 15, 11, 4)
