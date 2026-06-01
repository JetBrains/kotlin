

// WITH_STDLIB
// FILE: test.kt
fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                val y = "y"
            } finally {
                return "FAIL1"
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL2"
}

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// test.kt:7 box:
// test.kt:8 box: i:int=0:int
// test.kt:9 box: i:int=0:int
// test.kt:10 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:12 box: i:int=0:int
// test.kt:16 box:

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:7 box:
// test.kt:7 box:
// test.kt:7 box: i=0:number
// test.kt:9 box: i=0:number
// test.kt:10 box: i=0:number, x="x":kotlin.String
// test.kt:12 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
// test.kt:16 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String

// EXPECTATIONS WASM
// test.kt:7 $box: $i:i32=0:i32, $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (18, 8, 26, 8, 8, 18, 18, 18, 18, 18, 18)
// test.kt:9 $box: $i:i32=0:i32, $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (24, 24, 24, 24)
// test.kt:10 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (24, 24, 24, 24, 24, 24)
// test.kt:12 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (16, 23, 23, 23, 16)
// test.kt:16 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (8, 15, 15, 15, 8)
