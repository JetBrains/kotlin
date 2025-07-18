import kotlin.js.*

// some validation of allowed/prohibited JS names could be useful
@JsExport
fun `throw`(): Int = 1

fun box(): String {
    return "OK"
}
