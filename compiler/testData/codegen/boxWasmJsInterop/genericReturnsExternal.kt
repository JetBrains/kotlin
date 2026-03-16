// WITH_STDLIB

@file:Suppress("CAST_NEVER_SUCCEEDS_ERROR")

external interface EI

internal fun getAsJsString(): EI = getString() // <--- EI & JsAny = Any as EI

private fun <T : JsAny> getString(): T = js("'OK'")

fun box(): String {
    return (getAsJsString() as JsString).toString()
}
