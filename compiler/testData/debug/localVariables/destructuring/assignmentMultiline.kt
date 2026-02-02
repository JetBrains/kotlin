// WITH_STDLIB


// FILE: test.kt
fun box(): String {
    val p = "O" to "K"

    val
            (
        o
            ,
        k
    )
            =
        p

    return o + k
}

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:15 box: p:kotlin.Pair=kotlin.Pair
// EXPECTATIONS JVM_IR
// test.kt:10 box: p:kotlin.Pair=kotlin.Pair
// test.kt:12 box: p:kotlin.Pair=kotlin.Pair, o:java.lang.String="O":java.lang.String
// test.kt:17 box: p:kotlin.Pair=kotlin.Pair, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:6 box:
// test.kt:10 box: p=kotlin.Pair
// test.kt:12 box: p=kotlin.Pair, o="O":kotlin.String
// test.kt:17 box: p=kotlin.Pair, o="O":kotlin.String, k="K":kotlin.String

// EXPECTATIONS WASM
// test.kt:6 $box: $p:(ref null $kotlin.Pair)=null, $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (12, 12, 12, 19, 19, 19, 12)
// test.kt:15 $box: $p:(ref null $kotlin.Pair)=null, $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (8)
// test.kt:10 $box: $p:(ref $kotlin.Pair)=(ref $kotlin.Pair), $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:12 $box: $p:(ref $kotlin.Pair)=(ref $kotlin.Pair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref null $kotlin.String)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:17 $box: $p:(ref $kotlin.Pair)=(ref $kotlin.Pair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref $kotlin.String)=(ref $kotlin.String) (11, 15, 11, 4)
