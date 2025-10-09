// TARGET_BACKEND: WASM
// WASM_FAILS_IN_SINGLE_MODULE_MODE

external interface EI

internal fun getAsJsString(): EI = getString() // <--- EI & JsAny = Any as EI

private fun <T : JsAny> getString(): T = js("'OK'")

fun box(): String {
    return (getAsJsString() as JsString).toString()
}