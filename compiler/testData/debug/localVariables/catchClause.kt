// FILE: test.kt
fun box() {
    try {
        var a = 1
        a --
        a /= a
        throw ArithmeticException() // Division by 0 doesn't throw in JS, so throw explicitly
    } catch(e : Throwable) {
        e.printStackTrace()
    }
}

// EXPECTATIONS JVM_IR
// test.kt:3 box:
// test.kt:4 box:
// test.kt:5 box: a:int=1:int
// test.kt:6 box: a:int=0:int
// test.kt:8 box:
// test.kt:9 box: e:java.lang.Throwable=java.lang.ArithmeticException
// test.kt:11 box:

// EXPECTATIONS JS_IR
// test.kt:4 box:
// test.kt:5 box: a=1:number
// test.kt:6 box: a=0:number
// test.kt:7 box: a=0:number
// test.kt:8 box: a=0:number
// test.kt:8 box: a=0:number
// test.kt:9 box: a=0:number, e=kotlin.ArithmeticException
