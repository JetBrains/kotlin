// WITH_STDLIB


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

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:17 box: p:kotlin.Triple=kotlin.Triple
// EXPECTATIONS JVM_IR
// test.kt:12 box: p:kotlin.Triple=kotlin.Triple
// test.kt:14 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String
// test.kt:19 box: p:kotlin.Triple=kotlin.Triple, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:6 box:
// test.kt:12 box: p=kotlin.Triple
// test.kt:14 box: p=kotlin.Triple, o="O":kotlin.String
// test.kt:19 box: p=kotlin.Triple, o="O":kotlin.String, k="K":kotlin.String

// EXPECTATIONS WASM
// test.kt:6 $box: $p:(ref null $kotlin.Triple)=null, $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (12, 19, 19, 19, 23, 23, 23, 27, 27, 27, 12)
// test.kt:17 $box: $p:(ref null $kotlin.Triple)=null, $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (8)
// test.kt:12 $box: $p:(ref $kotlin.Triple)=(ref $kotlin.Triple), $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:14 $box: $p:(ref $kotlin.Triple)=(ref $kotlin.Triple), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref null $kotlin.String)=null (8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8)
// test.kt:19 $box: $p:(ref $kotlin.Triple)=(ref $kotlin.Triple), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref $kotlin.String)=(ref $kotlin.String) (11, 15, 11, 4)
