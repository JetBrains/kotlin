import kotlin.js.JsExport

@JsExport
suspend fun myBox(): String {
    return "OK"
}

fun box(): String {
    return "OK"
}