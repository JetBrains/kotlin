// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: -
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.4.20
// KT-49422: Fixed in 2.5.0-Beta2

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
