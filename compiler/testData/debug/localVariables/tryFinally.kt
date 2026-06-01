

// WITH_STDLIB

// FILE: test.kt
fun box() {
    var result = ""
    for (x in listOf("A", "B")) {
        try {
            val y = "y"
            result += y
            break
        }
        finally {
            val z = "z"
            result += z
        }
    }
}

// EXPECTATIONS JVM_IR
// test.kt:7 box:
// test.kt:8 box: result:java.lang.String="":java.lang.String
// test.kt:9 box: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:10 box: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:11 box: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:12 box: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:15 box: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:16 box: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, z:java.lang.String="z":java.lang.String
// test.kt:19 box: result:java.lang.String="yz":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:8 box: result="":kotlin.String
// test.kt:8 box: result="":kotlin.String
// test.kt:8 box: result="":kotlin.String
// test.kt:8 box: result="":kotlin.String
// test.kt:10 box: result="":kotlin.String, x="A":kotlin.String
// test.kt:11 box: result="":kotlin.String, x="A":kotlin.String, y="y":kotlin.String
// test.kt:12 box: result="y":kotlin.String, x="A":kotlin.String, y="y":kotlin.String
// test.kt:15 box: result="y":kotlin.String, x="A":kotlin.String, y="y":kotlin.String
// test.kt:16 box: result="y":kotlin.String, x="A":kotlin.String, y="y":kotlin.String, z="z":kotlin.String
// test.kt:19 box: result="yz":kotlin.String, x="A":kotlin.String, y="y":kotlin.String, z="z":kotlin.String

// EXPECTATIONS WASM
// test.kt:7 $box: $result:(ref null $kotlin.String)=null, $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null, $z:(ref null $kotlin.String)=null (17, 17, 17, 17)
// test.kt:8 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null, $z:(ref null $kotlin.String)=null (21, 21, 21, 21, 21, 21, 21, 26, 26, 26, 21, 21, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14)
// test.kt:10 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref null $kotlin.String)=null, $z:(ref null $kotlin.String)=null (20, 20, 20, 20)
// test.kt:11 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String), $z:(ref null $kotlin.String)=null (12, 22, 12, 12, 12)
// test.kt:12 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String), $z:(ref null $kotlin.String)=null (12)
// test.kt:16 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String), $z:(ref null $kotlin.String)=null (12)
// test.kt:15 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String), $z:(ref null $kotlin.String)=null (20, 20, 20, 20)
// test.kt:16 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String), $z:(ref null $kotlin.String)=null (12, 22, 12, 12, 12, 12)
// test.kt:19 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String), $z:(ref null $kotlin.String)=null (1, 1)
