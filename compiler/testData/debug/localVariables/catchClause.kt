// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
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

// EXPECTATIONS JVM JVM_IR
// test.kt:5 box:
// test.kt:6 box:
// test.kt:7 box: a:int=1:int
// test.kt:8 box: a:int=0:int
// test.kt:10 box:
// test.kt:11 box: e:java.lang.Throwable=java.lang.ArithmeticException
// test.kt:13 box:

// EXPECTATIONS JS_IR
// test.kt:6 box:
// test.kt:7 box: a=1:number
// test.kt:7 box: a=1:number
// test.kt:8 box: a=0:number
// test.kt:9 box: a=0:number
// test.kt:10 box: a=0:number
// test.kt:10 box: a=0:number
// test.kt:11 box: a=0:number, e=kotlin.ArithmeticException
