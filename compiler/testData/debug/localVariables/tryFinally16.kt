

// WITH_STDLIB
// FILE: test.kt
fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                val y = "y"
            } finally {
                return "FAIL1"
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL2"
}

// EXPECTATIONS JVM_IR
// test.kt:6 box:
// test.kt:7 box:
// test.kt:8 box: i:int=0:int
// test.kt:9 box: i:int=0:int
// test.kt:10 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:12 box: i:int=0:int
// test.kt:16 box:

// EXPECTATIONS JS_IR
// test.kt:7 box:
// test.kt:7 box:
// test.kt:7 box:
// test.kt:7 box: i=0:number
// test.kt:9 box: i=0:number
// test.kt:10 box: i=0:number, x="x":kotlin.String
// test.kt:12 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
// test.kt:16 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
