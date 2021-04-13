// FILE: test.kt

inline fun f(block: () -> Unit) {
    try {
        val z = 32
        for (j in 0 until 1) {
            return
        }
    } finally {
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
        val s2 = "OK"
        x = s2  // TODO: Why is `s2` not visible here?
    }
    return "FAIL"
}

fun box() {
    val result = compute()
    val localX = x
}

// The local variables `y` and `i` are visible in the finally block with old backend.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:32 box:
// test.kt:17 compute:
// test.kt:18 compute:
// test.kt:19 compute: y:int=42:int
// test.kt:20 compute: y:int=42:int, i:int=0:int
// test.kt:4 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:5 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:6 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, z$iv:int=32:int
// test.kt:7 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, z$iv:int=32:int, j$iv:int=0:int
// test.kt:10 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:21 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:25 compute:
// test.kt:26 compute:
// test.kt:32 box:
// test.kt:33 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:34 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String
