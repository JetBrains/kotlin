// IGNORE_INLINER: IR

// WITH_STDLIB
// FILE: test.kt

inline fun h(block: () -> Unit) {
    try {
        val hLocal = "hLocal"
        block()
    } finally {
        val h = "h"
    }
}

inline fun g(block: () -> Unit) {
    try {
        val gLocal = "gLocal"
        h(block)
    } finally {
        val g = "g"
    }
}

var x: String? = null

fun compute(): String {
    try {
        for (a in listOf("a")) {
            g {
                for (b in listOf("b")) {
                    return b
                }
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
// test.kt:42 box:
// test.kt:27 compute:
// test.kt:28 compute:
// test.kt:29 compute: a:java.lang.String="a":java.lang.String
// test.kt:16 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int
// test.kt:17 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int
// test.kt:18 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String
// test.kt:7 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int
// test.kt:8 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int
// test.kt:9 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int, hLocal$iv$iv:java.lang.String="hLocal":java.lang.String
// test.kt:30 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int, hLocal$iv$iv:java.lang.String="hLocal":java.lang.String, $i$a$-g-TestKt$compute$1:int=0:int
// test.kt:31 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int, hLocal$iv$iv:java.lang.String="hLocal":java.lang.String, $i$a$-g-TestKt$compute$1:int=0:int, b:java.lang.String="b":java.lang.String
// test.kt:11 compute: a:java.lang.String="a":java.lang.String
// test.kt:20 compute: a:java.lang.String="a":java.lang.String
// test.kt:36 compute:
// test.kt:42 box:
// test.kt:43 box: result:java.lang.String="b":java.lang.String
// test.kt:44 box: result:java.lang.String="b":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:42 box:
// test.kt:28 compute:
// test.kt:28 compute:
// test.kt:28 compute:
// test.kt:28 compute:
// test.kt:17 compute: a="a":kotlin.String
// test.kt:8 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String
// test.kt:30 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String
// test.kt:30 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String
// test.kt:30 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String
// test.kt:30 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String
// test.kt:31 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String, b="b":kotlin.String
// test.kt:11 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String, b="b":kotlin.String
// test.kt:20 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String, b="b":kotlin.String, h="h":kotlin.String
// test.kt:36 compute: a="a":kotlin.String, gLocal="gLocal":kotlin.String, hLocal="hLocal":kotlin.String, b="b":kotlin.String, h="h":kotlin.String, g="g":kotlin.String
