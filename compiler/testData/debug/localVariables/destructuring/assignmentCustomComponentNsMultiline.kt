// IGNORE_BACKEND_FIR: JVM_IR

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

// EXPECTATIONS
// test.kt:15 box:
// test.kt:4 <init>: x:java.lang.String="X":java.lang.String, y:java.lang.String="Y":java.lang.String
// test.kt:15 box:

// EXPECTATIONS JVM
// test.kt:23 box: p:MyPair=MyPair
// test.kt:6 component1:
// test.kt:18 box: p:MyPair=MyPair
// test.kt:10 component2:
// test.kt:20 box: p:MyPair=MyPair
// test.kt:24 box: p:MyPair=MyPair, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String

// EXPECTATIONS JVM_IR
// test.kt:23 box: p:MyPair=MyPair
// test.kt:6 component1:
// test.kt:18 box: p:MyPair=MyPair
// test.kt:10 component2:
// test.kt:20 box: p:MyPair=MyPair, o:java.lang.String="O":java.lang.String
// test.kt:24 box: p:MyPair=MyPair, o:java.lang.String="O":java.lang.String, k:java.lang.String="K":java.lang.String
