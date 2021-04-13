// FILE: test.kt

inline fun f(block: () -> Unit) {
    try {
        val z = 32
        for (j in 0 until 1) {
            throw RuntimeException("$z $j")
        }
    } catch (e: Exception) {
        block()
    }
}

var x: String = ""

fun compute(): String {
    try {
        val y = 42
        for (i in 0 until 1) {
            f {
                return "NON_LOCAL_RETURN"
            }
        }
    } finally {
        x = "OK"
    }
    return "FAIL"
}

fun box() {
    val result = compute()
    val localX = x
}

// The old backend has `y` and `i` visible on the finally block.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:31 box:
// test.kt:17 compute:
// test.kt:18 compute:
// test.kt:19 compute: y:int=42:int
// test.kt:20 compute: y:int=42:int, i:int=0:int
// test.kt:4 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:5 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:6 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, z$iv:int=32:int
// test.kt:7 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, z$iv:int=32:int, j$iv:int=0:int
// test.kt:9 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:10 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, e$iv:java.lang.Exception=java.lang.RuntimeException
// test.kt:21 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, e$iv:java.lang.Exception=java.lang.RuntimeException, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:25 compute:
// test.kt:31 box:
// test.kt:32 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:33 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String
