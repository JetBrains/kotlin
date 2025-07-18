import kotlin.js.*

// the current error seems to be unclear: OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE: Declaration annotated with '@OptionalExpectation' can only be used in common module sources. at (5,2) in /jsexport.kt
// maybe it could be improved by explicit prohibition to use JS-related features in WASI mode
@JsExport
fun myBox(): String {
    return "OK"
}

fun box(): String {
    return "OK"
}