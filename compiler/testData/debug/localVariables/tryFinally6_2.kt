
// WITH_STDLIB
// FILE: test.kt

inline fun g(block: () -> Unit) {
    try {
        val gLocal = "gLocal"
        block()
    } finally {
        val g = "g"
    }
}

var x: String? = null

fun compute(): String {
    try {
        g {
            for (b in listOf("b")) {
                return b
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
// test.kt:30 box:
// test.kt:17 compute:
// test.kt:18 compute:
// test.kt:6 compute: $i$f$g\1\18:int=0:int
// test.kt:7 compute: $i$f$g\1\18:int=0:int
// test.kt:8 compute: $i$f$g\1\18:int=0:int, gLocal\1:java.lang.String="gLocal":java.lang.String
// test.kt:19 compute: $i$f$g\1\18:int=0:int, gLocal\1:java.lang.String="gLocal":java.lang.String, $i$a$-g-TestKt$compute$1\2\90\0:int=0:int
// test.kt:20 compute: $i$f$g\1\18:int=0:int, gLocal\1:java.lang.String="gLocal":java.lang.String, $i$a$-g-TestKt$compute$1\2\90\0:int=0:int, b\2:java.lang.String="b":java.lang.String
// test.kt:10 compute:
// test.kt:24 compute:
// test.kt:30 box:
// test.kt:31 box: result:java.lang.String="b":java.lang.String
// test.kt:32 box: result:java.lang.String="b":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JVM_IR
// test.kt:30 box:
// test.kt:17 compute:
// test.kt:18 compute:
// test.kt:6 compute: $i$f$g:int=0:int
// test.kt:7 compute: $i$f$g:int=0:int
// test.kt:8 compute: $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String
// test.kt:19 compute: $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$a$-g-TestKt$compute$1:int=0:int
// test.kt:20 compute: $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$a$-g-TestKt$compute$1:int=0:int, b:java.lang.String="b":java.lang.String
// test.kt:10 compute:
// test.kt:24 compute:
// test.kt:30 box:
// test.kt:31 box: result:java.lang.String="b":java.lang.String
// test.kt:32 box: result:java.lang.String="b":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:30 box:
// test.kt:7 compute:
// test.kt:19 compute: gLocal="gLocal":kotlin.String
// test.kt:19 compute: gLocal="gLocal":kotlin.String
// test.kt:19 compute: gLocal="gLocal":kotlin.String
// test.kt:19 compute: gLocal="gLocal":kotlin.String
// test.kt:20 compute: gLocal="gLocal":kotlin.String, b="b":kotlin.String
// test.kt:10 compute: gLocal="gLocal":kotlin.String, b="b":kotlin.String
// test.kt:24 compute: gLocal="gLocal":kotlin.String, b="b":kotlin.String, g="g":kotlin.String

// EXPECTATIONS WASM
// test.kt:30 $box: $result:(ref null $kotlin.String)=null, $localX:(ref null $kotlin.String)=null (17)
// test.kt:18 $compute: $gLocal:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null, $g:(ref null $kotlin.String)=null (8)
// test.kt:7 $compute: $gLocal:(ref null $kotlin.String)=null, $b:(ref null $kotlin.String)=null, $g:(ref null $kotlin.String)=null (21, 21, 21, 21)
// test.kt:8 $compute: $gLocal:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref null $kotlin.String)=null, $g:(ref null $kotlin.String)=null (8)
// test.kt:19 $compute: $gLocal:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref null $kotlin.String)=null, $g:(ref null $kotlin.String)=null (29, 29, 29, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22)
// test.kt:20 $compute: $gLocal:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $g:(ref null $kotlin.String)=null (23, 16)
// test.kt:10 $compute: $gLocal:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $g:(ref null $kotlin.String)=null (16, 16, 16, 16, 16, 16, 16)
// test.kt:24 $compute: $gLocal:(ref $kotlin.String)=(ref $kotlin.String), $b:(ref $kotlin.String)=(ref $kotlin.String), $g:(ref null $kotlin.String)=null (8, 12, 12, 12, 8, 8, 8)
// test.kt:30 $box: $result:(ref null $kotlin.String)=null, $localX:(ref null $kotlin.String)=null (17)
// test.kt:31 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $localX:(ref null $kotlin.String)=null (17, 17)
// test.kt:32 $box: $result:(ref $kotlin.String)=(ref $kotlin.String), $localX:(ref $kotlin.String)=(ref $kotlin.String) (1, 1)
