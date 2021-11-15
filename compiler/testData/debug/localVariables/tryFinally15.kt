// WITH_STDLIB
// FILE: test.kt
fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                val y = "y"
            } finally {
                continue
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL"
}

// EXPECTATIONS
// test.kt:4 box:
// test.kt:5 box:
// test.kt:6 box: i:int=0:int
// test.kt:7 box: i:int=0:int
// test.kt:8 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:10 box: i:int=0:int
// EXPECTATIONS JVM
// test.kt:5 box: i:int=0:int
// EXPECTATIONS JVM_IR
// test.kt:5 box: i:int=0:int
// EXPECTATIONS
// test.kt:14 box:
