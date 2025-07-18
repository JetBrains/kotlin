import kotlin.js.JsExport

// please check whether exported suspend functions are OK for WASM (Zalim told me that they should be prohibited)
@JsExport
suspend fun myBox(): String {
    return "OK"
}

fun box(): String {
    return "OK"
}