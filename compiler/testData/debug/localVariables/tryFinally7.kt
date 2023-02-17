// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// The old backend has `y` and `j` visible on the finally block.
// IGNORE_BACKEND: JVM
// WITH_STDLIB
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

// EXPECTATIONS JVM JVM_IR
// test.kt:34 box:
// test.kt:15 compute:
// test.kt:16 compute:
// test.kt:17 compute: y:int=42:int
// test.kt:18 compute: y:int=42:int, i:int=0:int
// test.kt:20 compute:
// test.kt:21 compute: e:java.lang.Exception=java.lang.RuntimeException
// test.kt:22 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int
// test.kt:23 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int
// test.kt:9 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f:int=0:int
// test.kt:24 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f:int=0:int, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:28 compute:
// test.kt:34 box:
// test.kt:35 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:36 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:34 box:
// test.kt:16 compute:
// test.kt:17 compute: y=42:number
// test.kt:17 compute: y=42:number
// test.kt:17 compute: y=42:number
// test.kt:17 compute: y=42:number, i=0:number
// test.kt:18 compute: y=42:number, i=0:number
// test.kt:20 compute: y=42:number, i=0:number
// test.kt:20 compute: y=42:number, i=0:number
// test.kt:21 compute: y=42:number, i=0:number, e=kotlin.RuntimeException
// test.kt:22 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number
// test.kt:22 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number
// test.kt:22 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number
// test.kt:22 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number, j=0:number
// test.kt:24 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number, j=0:number
// test.kt:28 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number, j=0:number
