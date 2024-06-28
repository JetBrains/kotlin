

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
