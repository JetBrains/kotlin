// IGNORE_BACKEND: NATIVE
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: WASM
// missing isArrayOf on JS

// WITH_RUNTIME

fun box(): String {
    val array = listOf(2, 3, 9).toTypedArray()
    if (!array.isArrayOf<Int>()) return "fail: is not Array<Int>"

    val str = array.contentToString()
    if (str != "[2, 3, 9]") return str

    return "OK"
}
