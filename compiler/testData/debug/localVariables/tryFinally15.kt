// WITH_STDLIB
// FILE: test.kt
fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                val y = "y"
            } finally {
                continue
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL"
}

// EXPECTATIONS JVM_IR
// test.kt:4 box:
// test.kt:5 box:
// test.kt:6 box: i:int=0:int
// test.kt:7 box: i:int=0:int
// test.kt:8 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:10 box: i:int=0:int
// test.kt:5 box: i:int=0:int
// test.kt:14 box:

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:5 box:
// test.kt:5 box:
// test.kt:5 box: i=0:number
// test.kt:7 box: i=0:number
// test.kt:8 box: i=0:number, x="x":kotlin.String
// test.kt:10 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
// test.kt:5 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
// test.kt:14 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String

// EXPECTATIONS WASM
// test.kt:5 $box: $i:i32=0:i32, $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (18, 8, 26, 8, 8, 18, 18, 18, 18, 18, 18)
// test.kt:7 $box: $i:i32=0:i32, $x:(ref null $kotlin.String)=null, $y:(ref null $kotlin.String)=null (24, 24, 24, 24)
// test.kt:8 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (24, 24, 24, 24, 24, 24)
// test.kt:10 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (16, 16)
// test.kt:5 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (8, 26, 8, 8, 8, 8, 8)
// test.kt:14 $box: $i:i32=0:i32, $x:(ref $kotlin.String)=(ref $kotlin.String), $y:(ref $kotlin.String)=(ref $kotlin.String) (15, 8, 15, 15, 15, 8)
