// The old backend has the local y covering the finally block as well.
// IGNORE_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt
fun box() {
    var result = ""
    for (x in listOf("A", "B")) {
        try {
            val y = "y"
            result += y
            continue
        }
        finally {
            val z = "z"
            result += z
        }
    }
}

// EXPECTATIONS
// test.kt:6 box:
// test.kt:7 box: result:java.lang.String="":java.lang.String
// test.kt:8 box: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:9 box: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:10 box: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:11 box: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:14 box: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:15 box: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, z:java.lang.String="z":java.lang.String
// test.kt:7 box: result:java.lang.String="yz":java.lang.String
// test.kt:8 box: result:java.lang.String="yz":java.lang.String, x:java.lang.String="B":java.lang.String
// test.kt:9 box: result:java.lang.String="yz":java.lang.String, x:java.lang.String="B":java.lang.String
// test.kt:10 box: result:java.lang.String="yz":java.lang.String, x:java.lang.String="B":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:11 box: result:java.lang.String="yzy":java.lang.String, x:java.lang.String="B":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:14 box: result:java.lang.String="yzy":java.lang.String, x:java.lang.String="B":java.lang.String
// test.kt:15 box: result:java.lang.String="yzy":java.lang.String, x:java.lang.String="B":java.lang.String, z:java.lang.String="z":java.lang.String
// test.kt:7 box: result:java.lang.String="yzyz":java.lang.String
// test.kt:18 box: result:java.lang.String="yzyz":java.lang.String
