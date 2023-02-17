// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// The old backend has `y` and `i` visible on the finally block.
// IGNORE_BACKEND: JVM
// WITH_STDLIB
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

// EXPECTATIONS JVM JVM_IR
// test.kt:36 box:
// test.kt:22 compute:
// test.kt:23 compute:
// test.kt:24 compute: y:int=42:int
// test.kt:25 compute: y:int=42:int, i:int=0:int
// test.kt:9 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:10 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:11 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, z$iv:int=32:int
// test.kt:12 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, z$iv:int=32:int, j$iv:int=0:int
// test.kt:14 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int
// test.kt:15 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, e$iv:java.lang.Exception=java.lang.RuntimeException
// test.kt:26 compute: y:int=42:int, i:int=0:int, $i$f$f:int=0:int, e$iv:java.lang.Exception=java.lang.RuntimeException, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:30 compute:
// test.kt:36 box:
// test.kt:37 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:38 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:36 box:
// test.kt:23 compute:
// test.kt:24 compute: y=42:number
// test.kt:24 compute: y=42:number
// test.kt:24 compute: y=42:number
// test.kt:24 compute: y=42:number, i=0:number
// test.kt:10 compute: y=42:number, i=0:number
// test.kt:11 compute: y=42:number, i=0:number, z=32:number
// test.kt:11 compute: y=42:number, i=0:number, z=32:number
// test.kt:11 compute: y=42:number, i=0:number, z=32:number
// test.kt:11 compute: y=42:number, i=0:number, z=32:number, j=0:number
// test.kt:12 compute: y=42:number, i=0:number, z=32:number, j=0:number
// test.kt:14 compute: y=42:number, i=0:number, z=32:number, j=0:number
// test.kt:14 compute: y=42:number, i=0:number, z=32:number, j=0:number
// test.kt:26 compute: y=42:number, i=0:number, z=32:number, j=0:number, e=kotlin.RuntimeException
// test.kt:30 compute: y=42:number, i=0:number, z=32:number, j=0:number, e=kotlin.RuntimeException
