// WITH_STDLIB
// FILE: test.kt
fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                var y = "y"
            } finally {
                break
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL"
}

// EXPECTATIONS JVM_IR
// test.kt:4 box:
// test.kt:5 box:
// test.kt:6 box: i:int=0:int
// test.kt:7 box: i:int=0:int
// test.kt:8 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:10 box: i:int=0:int
// test.kt:14 box:

// EXPECTATIONS JS_IR
// test.kt:5 box:
// test.kt:5 box:
// test.kt:5 box:
// test.kt:5 box: i=0:number
// test.kt:7 box: i=0:number
// test.kt:8 box: i=0:number, x="x":kotlin.String
// test.kt:10 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
// test.kt:14 box: i=0:number, x="x":kotlin.String, y="y":kotlin.String
