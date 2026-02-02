

// WITH_STDLIB
// FILE: test.kt

fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                throw RuntimeException(x)
            } catch (e: Exception) {
                val y = "y"
                return "FAIL1"
            } finally {
                return "FAIL2"
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL3"
}

// EXPECTATIONS JVM_IR
// test.kt:7 box:
// test.kt:8 box:
// test.kt:9 box: i:int=0:int
// test.kt:10 box: i:int=0:int
// test.kt:11 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:12 box: i:int=0:int
// test.kt:13 box: i:int=0:int, e:java.lang.Exception=java.lang.RuntimeException
// test.kt:14 box: i:int=0:int, e:java.lang.Exception=java.lang.RuntimeException, y:java.lang.String="y":java.lang.String
// test.kt:16 box: i:int=0:int
// test.kt:20 box:

// EXPECTATIONS JS_IR
// test.kt:8 box:
// test.kt:8 box:
// test.kt:8 box:
// test.kt:8 box: i=0:number
// test.kt:10 box: i=0:number
// test.kt:11 box: i=0:number, x="x":kotlin.String
// test.kt:12 box: i=0:number, x="x":kotlin.String
// test.kt:12 box: i=0:number, x="x":kotlin.String
// test.kt:13 box: i=0:number, x="x":kotlin.String, e=kotlin.RuntimeException
// test.kt:14 box: i=0:number, x="x":kotlin.String, e=kotlin.RuntimeException, y="y":kotlin.String
// test.kt:16 box: i=0:number, x="x":kotlin.String, e=kotlin.RuntimeException, y="y":kotlin.String
// test.kt:20 box: i=0:number, x="x":kotlin.String, e=kotlin.RuntimeException, y="y":kotlin.String

// EXPECTATIONS WASM
// test.kt:8 $box: $i:i32=0:i32, $x:(ref null $kotlin.String)=null, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Exception)=null, $y:(ref null $kotlin.String)=null (18, 8, 26, 8, 8, 18, 18, 18, 18, 18, 18)
// test.kt:10 $box: $i:i32=0:i32, $x:(ref null $kotlin.String)=null, $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Exception)=null, $y:(ref null $kotlin.String)=null (24, 24, 24, 24)
// test.kt:11 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $merged_catch_param:(ref null $kotlin.Throwable)=null, $e:(ref null $kotlin.Exception)=null, $y:(ref null $kotlin.String)=null (22, 39, 22, 16)
// test.kt:13 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $y:(ref null $kotlin.String)=null (24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24)
// test.kt:14 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $y:(ref $kotlin.String)=(ref $kotlin.String) (23, 23, 23, 16)
// test.kt:16 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $y:(ref $kotlin.String)=(ref $kotlin.String) (16, 23, 23, 23, 16)
// test.kt:20 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $merged_catch_param:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $e:(ref $kotlin.RuntimeException)=(ref $kotlin.RuntimeException), $y:(ref $kotlin.String)=(ref $kotlin.String) (8, 15, 15, 15, 8)
