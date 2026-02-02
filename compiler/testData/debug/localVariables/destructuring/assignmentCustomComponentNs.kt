// FILE: test.kt
class MyPair(val x: String, val y: String) {
    operator fun component1(): String {
        return "O"
    }

    operator fun component2(): String {
        return "K"
    }
}

fun box(): String {
    val p = MyPair("X", "Y")
    val (o, k) = p
    return o + k
}

// EXPECTATIONS JVM_IR
// test.kt:13 box:
// test.kt:2 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:13 box:
// test.kt:14 box: p:MyPair=MyPair
// test.kt:4 component1:
// test.kt:14 box: p:MyPair=MyPair
// test.kt:8 component2:
// test.kt:14 box: p:MyPair=MyPair, o:java.lang.String="O":java.lang.String
// test.kt:15 box: p:MyPair=MyPair, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:13 box:
// test.kt:2 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:2 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:2 <init>: x="X":kotlin.String, y="Y":kotlin.String
// test.kt:14 box: p=MyPair
// test.kt:4 component1:
// test.kt:14 box: p=MyPair, o="O":kotlin.String
// test.kt:8 component2:
// test.kt:15 box: p=MyPair, o="O":kotlin.String, k="K":kotlin.String

// EXPECTATIONS WASM
// test.kt:13 $box: $p:(ref null $MyPair)=null, $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (12, 19, 19, 19, 24, 24, 24, 12)
// test.kt:2 $MyPair.<init>: $<this>:(ref $MyPair)=(ref $MyPair), $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 28, 28, 28, 42, 42, 42)
// test.kt:14 $box: $p:(ref $MyPair)=(ref $MyPair), $o:(ref null $kotlin.String)=null, $k:(ref null $kotlin.String)=null (17, 9, 9)
// test.kt:4 $MyPair.component1: $<this>:(ref $MyPair)=(ref $MyPair) (15, 15, 15, 8)
// test.kt:14 $box: $p:(ref $MyPair)=(ref $MyPair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref null $kotlin.String)=null (9, 12, 12)
// test.kt:8 $MyPair.component2: $<this>:(ref $MyPair)=(ref $MyPair) (15, 15, 15, 8)
// test.kt:14 $box: $p:(ref $MyPair)=(ref $MyPair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref null $kotlin.String)=null (12)
// test.kt:15 $box: $p:(ref $MyPair)=(ref $MyPair), $o:(ref $kotlin.String)=(ref $kotlin.String), $k:(ref $kotlin.String)=(ref $kotlin.String) (11, 15, 11, 4)
