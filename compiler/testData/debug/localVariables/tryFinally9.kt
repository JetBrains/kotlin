

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

// EXPECTATIONS JVM_IR +USE_INLINE_SCOPES_NUMBERS
// test.kt:37 box:
// test.kt:13 compute:
// test.kt:14 compute:
// test.kt:15 compute:
// test.kt:16 compute: y:int=42:int
// test.kt:17 compute: y:int=42:int, i:int=0:int
// test.kt:20 compute:
// test.kt:21 compute: s:java.lang.String="NOPE":java.lang.String
// test.kt:22 compute: s:java.lang.String="NOPE":java.lang.String
// test.kt:7 compute: s:java.lang.String="NOPE":java.lang.String, $i$f$f\1\22:int=0:int
// test.kt:23 compute: s:java.lang.String="NOPE":java.lang.String, $i$f$f\1\22:int=0:int, $i$a$-f-TestKt$compute$1\2\124\0:int=0:int
// test.kt:27 compute:
// test.kt:28 compute: s2:java.lang.String="NOPE":java.lang.String
// test.kt:29 compute: s2:java.lang.String="NOPE":java.lang.String, j:int=0:int
// test.kt:28 compute: s2:java.lang.String="OK":java.lang.String, j:int=0:int
// test.kt:31 compute: s2:java.lang.String="OK":java.lang.String
// test.kt:37 box:
// test.kt:38 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String
// test.kt:39 box: result:java.lang.String="NON_LOCAL_RETURN":java.lang.String, localX:java.lang.String="OK":java.lang.String

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

// EXPECTATIONS WASM
// test.kt:37 $box: $result:(ref null $kotlin.String)=null, $localX:(ref null $kotlin.String)=null (17)
// test.kt:15 $compute: $y:i32=0:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (20, 20)
// test.kt:16 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (22, 12, 30, 12, 12, 22, 22, 22, 22, 22, 22, 22, 22)
// test.kt:17 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (23, 23, 23, 16)
// test.kt:8 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (1)
// test.kt:20 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (20, 20, 20)
// test.kt:21 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (16, 12)
// test.kt:22 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (12)
// test.kt:7 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (4)
// test.kt:23 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (23, 23, 23, 16)
// test.kt:31 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (8)
// test.kt:27 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (17, 17, 17, 17)
// test.kt:28 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (18, 8, 26, 8, 8, 18, 18, 18, 18, 18, 18, 18, 18)
// test.kt:29 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (17, 17, 17, 12, 12)
// test.kt:28 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (8, 26, 8, 8, 8, 8)
// test.kt:31 $compute: $y:i32=42:i32, $i:i32=0:i32, $s:(ref null $kotlin.String)=null, $s2:(ref null $kotlin.String)=null, $j:i32=0:i32 (12, 12, 8, 8, 8)
// test.kt:37 $box: $result:(ref null $kotlin.String)=null, $localX:(ref null $kotlin.String)=null (17)
// test.kt:38 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $localX:(ref null $kotlin.String)=null (17, 17)
// test.kt:39 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $localX:(ref $kotlin.String)=(ref $kotlin.String) (1, 1)
