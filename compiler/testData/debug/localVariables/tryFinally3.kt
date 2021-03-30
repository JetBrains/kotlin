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

// The old backend has the local y covering the finally block as well.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:20 box:
// test.kt:4 compute:
// test.kt:5 compute: result:java.lang.String="":java.lang.String
// test.kt:6 compute: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:7 compute: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:8 compute: result:java.lang.String="":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:9 compute: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:12 compute: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String
// test.kt:13 compute: result:java.lang.String="y":java.lang.String, x:java.lang.String="A":java.lang.String, z:java.lang.String="z":java.lang.String
// test.kt:9 compute: result:java.lang.String="yz":java.lang.String, x:java.lang.String="A":java.lang.String, y:java.lang.String="y":java.lang.String
// test.kt:20 box:
// test.kt:21 box:
