// IGNORE_BACKEND: WASM
// WASM_MUTE_REASON: IGNORED_IN_JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

// WITH_STDLIB

fun <T> test1() = null as T
fun <T> test2(): T {
   val a : Any? = null
   return a as T
}

fun <T: Any> test3() = null as T

fun box(): String {
    if (test1<Int?>() != null) return "fail: test1"
    if (test2<Int?>() != null) return "fail: test2"
    var result3 = "fail"
    try {
        test3<Int>()
    }
    catch(e: NullPointerException) {
        result3 = "OK"
    }
    if (result3 != "OK") return "fail: test3"
    return "OK"
}
