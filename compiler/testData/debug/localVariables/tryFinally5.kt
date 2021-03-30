// FILE: test.kt

inline fun g(block: () -> Unit) {
    block()
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

// JVM_IR backend correctly gets rid of the `a` local in the finally, but the inliner
// does not get rid of `b` or the inlining locals.

// JVM backend does not get rid of `a` and also the inliner does not get rid of `b`
// or the inlining locals.

// IGNORE_BACKEND: JVM_IR, JVM

// LOCAL VARIABLES
// test.kt:25 box:
// test.kt:10 compute:
// test.kt:11 compute:
// test.kt:12 compute: a:java.lang.String="a":java.lang.String
// test.kt:4 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int
// test.kt:13 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, $i$a$-g-TestKt$compute$1:int=0:int
// test.kt:14 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, $i$a$-g-TestKt$compute$1:int=0:int, b:java.lang.String="b":java.lang.String
// test.kt:19 compute:
// test.kt:25 box:
// test.kt:26 box: result:java.lang.String="b":java.lang.String
// test.kt:27 box: result:java.lang.String="b":java.lang.String, localX:java.lang.String="OK":java.lang.String
