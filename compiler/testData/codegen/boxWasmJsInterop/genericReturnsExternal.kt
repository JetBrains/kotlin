// TARGET_BACKEND: WASM

external interface EI

internal fun getAsJsString(): EI = getString() // <--- EI & JsAny = Any as EI

private fun <T : JsAny> getString(): T = js("'OK'")

fun box(): String {
    return (getAsJsString() as JsString).toString()
}