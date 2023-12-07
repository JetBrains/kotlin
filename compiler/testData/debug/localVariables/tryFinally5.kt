

// WITH_STDLIB
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

// EXPECTATIONS JVM_IR
// test.kt:28 box:
// test.kt:13 compute:
// test.kt:14 compute:
// test.kt:15 compute: a:java.lang.String="a":java.lang.String
// test.kt:7 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int
// test.kt:16 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, $i$a$-g-TestKt$compute$1:int=0:int
// test.kt:17 compute: a:java.lang.String="a":java.lang.String, $i$f$g:int=0:int, $i$a$-g-TestKt$compute$1:int=0:int, b:java.lang.String="b":java.lang.String
// test.kt:22 compute:
// test.kt:28 box:
// test.kt:29 box: result:java.lang.String="b":java.lang.String
// test.kt:30 box: result:java.lang.String="b":java.lang.String, localX:java.lang.String="OK":java.lang.String

// EXPECTATIONS JS_IR
// test.kt:28 box:
// test.kt:14 compute:
// test.kt:14 compute:
// test.kt:14 compute:
// test.kt:14 compute:
// test.kt:16 compute: a="a":kotlin.String
// test.kt:16 compute: a="a":kotlin.String
// test.kt:16 compute: a="a":kotlin.String
// test.kt:16 compute: a="a":kotlin.String
// test.kt:17 compute: a="a":kotlin.String, b="b":kotlin.String
// test.kt:22 compute: a="a":kotlin.String, b="b":kotlin.String
