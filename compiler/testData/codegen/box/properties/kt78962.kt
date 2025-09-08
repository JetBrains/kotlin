// TARGET_BACKEND: JS_IR, JS_IR_ES6, WASM, NATIVE

@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
val x = 42 as Any as Int

fun box(): String {
    return if (x == 42) "OK" else "FAIL"
}