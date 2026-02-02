

// FILE: test.kt
fun box(): String {
    val
            o
            =
        "O"


    val k = "K"

    return o + k
}

// EXPECTATIONS JVM_IR
// test.kt:8 box:
// EXPECTATIONS ClassicFrontend JVM_IR
// test.kt:6 box:
// EXPECTATIONS FIR JVM_IR
// test.kt:5 box:
// EXPECTATIONS JVM_IR
// test.kt:11 box: o:java.lang.String="O":java.lang.String
// test.kt:13 box: o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:11 box: o="O":kotlin.String
// test.kt:13 box: o="O":kotlin.String, k="K":kotlin.String

// EXPECTATIONS WASM
// test.kt:8 $box: $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (8, 8, 8, 8)
// test.kt:11 $box: $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref null $kotlin.String)=null (12, 12, 12, 12)
// test.kt:13 $box: $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref $kotlin.String)=(ref $kotlin.String) (11, 15, 11, 4)
