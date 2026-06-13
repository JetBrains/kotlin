// DONT_TARGET_EXACT_BACKEND: WASM_JS, WASM_WASI
// Due to the way the D8BasedDebugRunner works (using the D8 tool and its chrome debugger API to step into every statement until box() finishes execution), this test takes ages on wasm (5min locally), as the debugger stops at every step of the js exception handling. Disable it for now
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
// test.kt:5 box:
// test.kt:6 box:
// test.kt:7 box: a:int=1:int
// test.kt:8 box: a:int=0:int
// test.kt:10 box:
// test.kt:11 box: e:java.lang.Throwable=java.lang.ArithmeticException
// test.kt:13 box:

// EXPECTATIONS JS_IR
// test.kt:4 box:
// test.kt:5 box: a=1:number
// test.kt:6 box: a=0:number
// test.kt:7 box: a=0:number
// test.kt:8 box: a=0:number
// test.kt:8 box: a=0:number
// test.kt:9 box: a=0:number, e=kotlin.ArithmeticException
