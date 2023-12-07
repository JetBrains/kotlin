// IGNORE_INLINER: IR
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
