

// WITH_STDLIB
// FILE: test.kt

fun compute(): String {
    var result = ""
    try {
        val a = "a"
        try {
            val b = "b"
            for (i in 0 until 1) {
                val e = "e"
                result += b
                return result
            }
        } finally {
            val c = "c"
            result += c
        }
    } finally {
        val d = "d"
        result += d
    }
    return result
}

fun box() {
    compute()
}

// EXPECTATIONS JVM_IR
// test.kt:29 box:
// test.kt:7 compute:
// test.kt:8 compute: result:java.lang.String="":java.lang.String
// test.kt:9 compute: result:java.lang.String="":java.lang.String
// test.kt:10 compute: result:java.lang.String="":java.lang.String, a:java.lang.String="a":java.lang.String
// test.kt:11 compute: result:java.lang.String="":java.lang.String, a:java.lang.String="a":java.lang.String
// test.kt:12 compute: result:java.lang.String="":java.lang.String, a:java.lang.String="a":java.lang.String, b:java.lang.String="b":java.lang.String
// test.kt:13 compute: result:java.lang.String="":java.lang.String, a:java.lang.String="a":java.lang.String, b:java.lang.String="b":java.lang.String, i:int=0:int
// test.kt:14 compute: result:java.lang.String="":java.lang.String, a:java.lang.String="a":java.lang.String, b:java.lang.String="b":java.lang.String, i:int=0:int, e:java.lang.String="e":java.lang.String
// test.kt:15 compute: result:java.lang.String="b":java.lang.String, a:java.lang.String="a":java.lang.String, b:java.lang.String="b":java.lang.String, i:int=0:int, e:java.lang.String="e":java.lang.String
// test.kt:18 compute: result:java.lang.String="b":java.lang.String, a:java.lang.String="a":java.lang.String
// test.kt:19 compute: result:java.lang.String="b":java.lang.String, a:java.lang.String="a":java.lang.String, c:java.lang.String="c":java.lang.String
// test.kt:22 compute: result:java.lang.String="bc":java.lang.String
// test.kt:23 compute: result:java.lang.String="bc":java.lang.String, d:java.lang.String="d":java.lang.String
// test.kt:15 compute: result:java.lang.String="bcd":java.lang.String, a:java.lang.String="a":java.lang.String, b:java.lang.String="b":java.lang.String, i:int=0:int, e:java.lang.String="e":java.lang.String
// test.kt:29 box:
// test.kt:30 box:

// EXPECTATIONS JS_IR
// test.kt:29 box:
// test.kt:7 compute:
// test.kt:9 compute: result="":kotlin.String
// test.kt:11 compute: result="":kotlin.String, a="a":kotlin.String
// test.kt:12 compute: result="":kotlin.String, a="a":kotlin.String, b="b":kotlin.String
// test.kt:12 compute: result="":kotlin.String, a="a":kotlin.String, b="b":kotlin.String
// test.kt:12 compute: result="":kotlin.String, a="a":kotlin.String, b="b":kotlin.String
// test.kt:12 compute: result="":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number
// test.kt:13 compute: result="":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number
// test.kt:14 compute: result="":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number, e="e":kotlin.String
// test.kt:15 compute: result="b":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number, e="e":kotlin.String
// test.kt:18 compute: result="b":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number, e="e":kotlin.String
// test.kt:19 compute: result="b":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number, e="e":kotlin.String, c="c":kotlin.String
// test.kt:22 compute: result="bc":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number, e="e":kotlin.String, c="c":kotlin.String
// test.kt:23 compute: result="bc":kotlin.String, a="a":kotlin.String, b="b":kotlin.String, i=0:number, e="e":kotlin.String, c="c":kotlin.String, d="d":kotlin.String

// EXPECTATIONS WASM
// test.kt:29 $box: (4)
// test.kt:7 $compute: $result:(ref null $kotlin.String)=null, $a:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null, $i:i32=0:i32, $e:(ref null $kotlin.String)=null, $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (17, 17, 17, 17)
// test.kt:9 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null, $i:i32=0:i32, $e:(ref null $kotlin.String)=null, $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (16, 16, 16, 16)
// test.kt:11 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref null $kotlin.String)=null, $i:i32=0:i32, $e:(ref null $kotlin.String)=null, $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (20, 20, 20, 20)
// test.kt:12 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref null $kotlin.String)=null, $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (22, 12, 30, 12, 12, 22, 22, 22, 22, 22, 22, 22, 22)
// test.kt:13 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref null $kotlin.String)=null, $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (24, 24, 24, 24)
// test.kt:14 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (16, 26, 16, 16)
// test.kt:15 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (23, 16)
// test.kt:19 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (12)
// test.kt:18 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (20, 20, 20, 20)
// test.kt:19 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (12, 22, 12, 12, 12, 12)
// test.kt:23 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (8)
// test.kt:22 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (16, 16, 16, 16)
// test.kt:23 $compute: $result:(ref $kotlin.String)=(ref $kotlin.String), $a:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $i:i32=0:i32, $e:(ref $kotlin.String)=(ref $kotlin.String), $c:(ref null $kotlin.String)=null, $d:(ref null $kotlin.String)=null (8, 18, 8, 8, 8, 8)
// test.kt:29 $box: (4)
// test.kt:30 $box: (1)
