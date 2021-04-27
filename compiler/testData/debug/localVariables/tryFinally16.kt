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

// The local `i` is visible in the finally block with old backend.
// IGNORE_BACKEND: JVM

// LOCAL VARIABLES
// test.kt:4 box:
// test.kt:5 box:
// test.kt:6 box: i:int=0:int
// test.kt:7 box: i:int=0:int
// test.kt:8 box: i:int=0:int, x:java.lang.String="x":java.lang.String
// test.kt:10 box: i:int=0:int
// test.kt:14 box:
