// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_NON_REVERSED_RESOLVE: KT-62937
// LANGUAGE: -JsAllowInvalidCharsIdentifiersEscaping
private fun ` .private `(): String = TODO("")

fun ` .public `(): String = TODO("")

<!NAME_CONTAINS_ILLEGAL_CHARS!>@JsName("  __  ")
fun foo(): String<!> = TODO("")

<!NAME_CONTAINS_ILLEGAL_CHARS!>@JsName("  ___  ")
private fun bar(): String<!> = TODO("")

@JsName("validName")
private fun ` .private with @JsName `(): String = TODO("")

private class ` .private class ` {
    val ` .field. ` = ""
}

val x: Int
    <!NAME_CONTAINS_ILLEGAL_CHARS!>@JsName(".")
    get()<!> = TODO("")

fun box(x: dynamic) {
    x.<!NAME_CONTAINS_ILLEGAL_CHARS!>`foo-bar`<!>()
    x.<!NAME_CONTAINS_ILLEGAL_CHARS!>`ba-z`<!>
}
