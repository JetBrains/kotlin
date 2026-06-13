

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

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:32 box:
// test.kt:13 compute:
// test.kt:14 compute:
// test.kt:15 compute: y:int=42:int
// test.kt:16 compute: y:int=42:int, i:int=0:int
// test.kt:18 compute:
// test.kt:19 compute: e:java.lang.Exception=java.lang.RuntimeException
// test.kt:20 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int
// test.kt:21 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int
// test.kt:7 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f\1\21:int=0:int
// test.kt:22 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f\1\21:int=0:int, $i$a$-f-TestKt$compute$1\2\103\0:int=0:int
// test.kt:26 compute:
// test.kt:32 box:
// test.kt:33 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:34 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JVM_IR
// test.kt:32 box:
// test.kt:13 compute:
// test.kt:14 compute:
// test.kt:15 compute: y:int=42:int
// test.kt:16 compute: y:int=42:int, i:int=0:int
// test.kt:18 compute:
// test.kt:19 compute: e:java.lang.Exception=java.lang.RuntimeException
// test.kt:20 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int
// test.kt:21 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int
// test.kt:7 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f:int=0:int
// test.kt:22 compute: e:java.lang.Exception=java.lang.RuntimeException, y:int=32:int, j:int=0:int, $i$f$f:int=0:int, $i$a$-f-TestKt$compute$1:int=0:int
// test.kt:26 compute:
// test.kt:32 box:
// test.kt:33 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:34 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:32 box:
// test.kt:14 compute:
// test.kt:15 compute: y=42:number
// test.kt:15 compute: y=42:number
// test.kt:15 compute: y=42:number
// test.kt:15 compute: y=42:number, i=0:number
// test.kt:16 compute: y=42:number, i=0:number
// test.kt:18 compute: y=42:number, i=0:number
// test.kt:18 compute: y=42:number, i=0:number
// test.kt:19 compute: y=42:number, i=0:number, e=kotlin.RuntimeException
// test.kt:20 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number
// test.kt:20 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number
// test.kt:20 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number
// test.kt:20 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number, j=0:number
// test.kt:22 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number, j=0:number
// test.kt:26 compute: y=42:number, i=0:number, e=kotlin.RuntimeException, y=32:number, j=0:number

// EXPECTATIONS WASM
// test.kt:32 $box: $result:(ref null $kotlin.String)=null, $localX:(ref null $kotlin.String)=null (17)
// test.kt:14 $compute: $y:i32=0:i32, $i:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Exception)=null, $j:i32=0:i32 (16, 16)
// test.kt:15 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Exception)=null, $j:i32=0:i32 (18, 8, 26, 8, 8, 18, 18, 18, 18, 18, 18)
// test.kt:16 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Exception)=null, $j:i32=0:i32 (18, 37, 37, 37, 37, 37, 37, 35, 35, 35, 35, 35, 38, 38, 38, 35, 40, 40, 40, 40, 40, 40, 35, 18, 12)
// test.kt:19 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $j:i32=0:i32 (16, 16, 16, 16, 16, 16, 16, 16, 16)
// test.kt:20 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $j:i32=0:i32 (18, 8, 26, 8, 8, 18, 18, 18, 18, 18, 18)
// test.kt:21 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $j:i32=0:i32 (12)
// test.kt:7 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $j:i32=0:i32 (4)
// test.kt:22 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $j:i32=0:i32 (23, 23, 23, 16)
// test.kt:26 $compute: $y:i32=42:i32, $i:i32=0:i32, $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $j:i32=0:i32 (8, 12, 12, 12, 8, 8, 8)
// test.kt:32 $box: $result:(ref null $kotlin.String)=null, $localX:(ref null $kotlin.String)=null (17)
// test.kt:33 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $localX:(ref null $kotlin.String)=null (17, 17)
// test.kt:34 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $localX:(ref $kotlin.String)=(ref $kotlin.String) (1, 1)
