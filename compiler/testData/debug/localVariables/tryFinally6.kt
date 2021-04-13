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

// JVM backend has `a` visible in the `compute` finally block. It shouldn't be.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:39 box:
// test.kt:24 compute:
// test.kt:25 compute:
// test.kt:26 compute: a:java.lang.String="a":java.lang.String
// test.kt:13 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int
// test.kt:14 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int
// test.kt:15 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String
// test.kt:4 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int
// test.kt:5 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int
// test.kt:6 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int, hLocal$iv$iv:java.lang.String="hLocal":java.lang.String
// test.kt:27 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int, hLocal$iv$iv:java.lang.String="hLocal":java.lang.String, $i$a$-g-TestKt$compute$1:int=0:int
// test.kt:28 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, gLocal$iv:java.lang.String="gLocal":java.lang.String, $i$f$h:int=0:int, hLocal$iv$iv:java.lang.String="hLocal":java.lang.String, $i$a$-g-TestKt$compute$1:int=0:int, b:java.lang.String="b":java.lang.String
// test.kt:8 compute: a:java.lang.String="a":java.lang.String
// test.kt:17 compute: a:java.lang.String="a":java.lang.String
// test.kt:33 compute:
// test.kt:39 box:
// test.kt:40 box: result:java.lang.String="b":java.lang.String
// test.kt:41 box: result:java.lang.String="b":java.lang.String, localX:java.lang.String="OK":java.lang.String
