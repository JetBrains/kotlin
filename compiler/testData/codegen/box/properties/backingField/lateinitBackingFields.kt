// TARGET_BACKEND: JVM_IR
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: JS_IR_ES6
// TARGET_BACKEND: WASM
// IGNORE_BACKEND_K1: JVM_IR, JS_IR, JS_IR_ES6, WASM

var that: Int
    lateinit field: String
    get() = field.length
    set(value) {
        field = value.toString()
    }

fun box(): String {
    that = 1

    return if (that == 1) {
        "OK"
    } else {
        "fail: $that"
    }
}
