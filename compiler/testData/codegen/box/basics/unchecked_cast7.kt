// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_1_9
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0 does not know this option
// IGNORE_BACKEND: JS_IR, JS_IR_ES6, WASM

fun box(): String {
    try {
        val x = Any().uncheckedCast<Int?>()
        return "FAIL: $x"
    } catch (e: ClassCastException) {
        return "OK"
    }
}

fun <T> Any?.uncheckedCast(): T = this as T