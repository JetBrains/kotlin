// FILE: test.kt

inline fun f(block: () -> Unit) {
    block()
}

var x: String = ""

fun compute(): String {
    try {
        val y = 42
        for (i in 0 until 1) {
            throw RuntimeException("$y $i")
        }
    } catch (e: Exception) {
        val y = 32
        for (j in 0 until 1) {
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

// The old backend has `y` and `j` visible on the finally block.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:29 box:
// test.kt:10 compute:
// test.kt:11 compute:
// test.kt:12 compute: y:int=42:int
// test.kt:13 compute: y:int=42:int, i:int=0:int
// test.kt:15 compute:
// test.kt:16 compute: e:java.lang.Exception=java.lang.RuntimeException
// test.kt:17 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int
// test.kt:18 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int
// test.kt:4 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f:int=0:int
// test.kt:19 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f:int=0:int, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:23 compute:
// test.kt:29 box:
// test.kt:30 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:31 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String
