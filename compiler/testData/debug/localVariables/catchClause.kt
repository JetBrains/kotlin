// FILE: test.kt
fun box() {
    try {
        var a = 1
        a --
        a /= a
    } catch(e : Throwable) {
        e.printStackTrace()
    }
}

// LOCAL VARIABLES
// test.kt:3 box:
// test.kt:4 box:
// test.kt:5 box: a:int=1:int
// test.kt:6 box: a:int=0:int
// test.kt:7 box:
// test.kt:8 box: e:java.lang.Throwable=java.lang.ArithmeticException
// test.kt:10 box:
