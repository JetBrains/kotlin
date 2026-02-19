

// WITH_STDLIB
// FILE: test.kt

fun compute(): String {
    var result = ""
    for (x in listOf("A", "B")) {
        try {
            val y = "y"
            result += y
            return result
        }
        finally {
            val z = "z"
            result += z
        }
    }
    return result
}

fun box() {
    compute()
}

// EXPECTATIONS JVM_IR
// test.kt:23 box:
// test.kt:7 compute:
// test.kt:8 compute: result:java.lang.String="":java.lang.String
// test.kt:9 compute: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:10 compute: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:11 compute: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:12 compute: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:15 compute: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:16 compute: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, z:java.lang.String="z":java.lang.String
// test.kt:12 compute: result:java.lang.String="yz":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:23 box:
// test.kt:24 box:

// EXPECTATIONS JS_IR
// test.kt:23 box:
// test.kt:7 compute:
// test.kt:8 compute: result="":kotlin.String
// test.kt:8 compute: result="":kotlin.String
// test.kt:8 compute: result="":kotlin.String
// test.kt:8 compute: result="":kotlin.String
// test.kt:10 compute: result="":kotlin.String, x="A":kotlin.String
// test.kt:11 compute: result="":kotlin.String, x="A":kotlin.String, y="y":kotlin.String
// test.kt:12 compute: result="y":kotlin.String, x="A":kotlin.String, y="y":kotlin.String
// test.kt:15 compute: result="y":kotlin.String, x="A":kotlin.String, y="y":kotlin.String
// test.kt:16 compute: result="y":kotlin.String, x="A":kotlin.String, y="y":kotlin.String, z="z":kotlin.String
