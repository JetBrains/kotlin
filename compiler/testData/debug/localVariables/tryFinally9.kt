// FILE: test.kt

inline fun f(block: () -> Unit) {
    block()
}

var x: String = ""

fun compute(): String {
    try {
        try {
            val y = 42
            for (i in 0 until 1) {
                return "NORMAL_RETURN"
            }
        } finally {
            var s = "NOPE"
            x = s
            f {
                return "NON_LOCAL_RETURN"
            }
        }
    } finally {
        var s2 = "NOPE"
        for (j in 0 until 1) {
            s2 = "OK"
        }
        x = s2
    }
    return "FAIL"
}

fun box() {
    val result = compute()
    val localX = x
}

// The local variables in the try and finally blocks are not removed for the finally block with the old backend.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:34 box:
// test.kt:10 compute:
// test.kt:11 compute:
// test.kt:12 compute:
// test.kt:13 compute: y:int=42:int
// test.kt:14 compute: y:int=42:int, i:int=0:int
// test.kt:17 compute:
// test.kt:18 compute: s:java.lang.String="NOPE":java.lang.String
// test.kt:19 compute: s:java.lang.String="NOPE":java.lang.String
// test.kt:4 compute: s:java.lang.String="NOPE":java.lang.String, $i$f$f:int=0:int
// test.kt:20 compute: s:java.lang.String="NOPE":java.lang.String, $i$f$f:int=0:int, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:24 compute:
// test.kt:25 compute: s2:java.lang.String="NOPE":java.lang.String
// test.kt:26 compute: s2:java.lang.String="NOPE":java.lang.String, j:int=0:int
// test.kt:25 compute: s2:java.lang.String="OK":java.lang.String, j:int=0:int
// test.kt:28 compute: s2:java.lang.String="OK":java.lang.String
// test.kt:34 box:
// test.kt:35 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:36 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String
