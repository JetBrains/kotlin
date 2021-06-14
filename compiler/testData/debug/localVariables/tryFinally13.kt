// FILE: test.kt

fun box(): String {
    try {
        for (i in 0 until 1) {
            try {
                val x = "x"
                throw RuntimeException(x)
            } catch (e: Exception) {
                val y = "y"
                return "FAIL1"
            } finally {
                return "FAIL2"
            }
        }
    } finally {
        return "OK"
    }
    return "FAIL3"
}

// The local variables `y` and `i` are visible in finally blocks with old backend.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:4 box:
// test.kt:5 box:
// test.kt:6 box: i:int=0:int
// test.kt:7 box: i:int=0:int
// test.kt:8 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:9 box: i:int=0:int
// test.kt:10 box: i:int=0:int, e:java.lang.Exception=java.lang.RuntimeException
// test.kt:11 box: i:int=0:int, e:java.lang.Exception=java.lang.RuntimeException, y:java.lang.String="y":java.lang.String
// test.kt:13 box: i:int=0:int
// test.kt:17 box:
