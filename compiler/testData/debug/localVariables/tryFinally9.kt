

// WITH_STDLIB
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

// EXPECTATIONS JVM_IR
// test.kt:37 box:
// test.kt:13 compute:
// test.kt:14 compute:
// test.kt:15 compute:
// test.kt:16 compute: y:int=42:int
// test.kt:17 compute: y:int=42:int, i:int=0:int
// test.kt:20 compute:
// test.kt:21 compute: s:java.lang.String="NOPE":java.lang.String
// test.kt:22 compute: s:java.lang.String="NOPE":java.lang.String
// test.kt:7 compute: s:java.lang.String="NOPE":java.lang.String, $i$f$f:int=0:int
// test.kt:23 compute: s:java.lang.String="NOPE":java.lang.String, $i$f$f:int=0:int, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:27 compute:
// test.kt:28 compute: s2:java.lang.String="NOPE":java.lang.String
// test.kt:29 compute: s2:java.lang.String="NOPE":java.lang.String, j:int=0:int
// test.kt:28 compute: s2:java.lang.String="OK":java.lang.String, j:int=0:int
// test.kt:31 compute: s2:java.lang.String="OK":java.lang.String
// test.kt:37 box:
// test.kt:38 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:39 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:37 box:
// test.kt:15 compute:
// test.kt:16 compute: y=42:number
// test.kt:16 compute: y=42:number
// test.kt:16 compute: y=42:number
// test.kt:16 compute: y=42:number, i=0:number
// test.kt:17 compute: y=42:number, i=0:number
// test.kt:20 compute: y=42:number, i=0:number
// test.kt:21 compute: y=42:number, i=0:number, s="NOPE":kotlin.String
// test.kt:23 compute: y=42:number, i=0:number, s="NOPE":kotlin.String
// test.kt:27 compute: y=42:number, i=0:number, s="NOPE":kotlin.String
// test.kt:28 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="NOPE":kotlin.String
// test.kt:28 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="NOPE":kotlin.String
// test.kt:28 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="NOPE":kotlin.String
// test.kt:28 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="NOPE":kotlin.String, j=0:number
// test.kt:29 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="NOPE":kotlin.String, j=0:number
// test.kt:28 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="OK":kotlin.String, j=0:number
// test.kt:31 compute: y=42:number, i=0:number, s="NOPE":kotlin.String, s2="OK":kotlin.String, j=0:number
