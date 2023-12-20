// !LANGUAGE: -JsAllowInvalidCharsIdentifiersEscaping
// FIR_IDENTICAL
private fun ` .private `(): String = TODO("")

<!NAME_CONTAINS_ILLEGAL_CHARS!>fun ` .public `(): String<!> = TODO("")

<!NAME_CONTAINS_ILLEGAL_CHARS!>@JsName("  __  ")
fun foo(): String<!> = TODO("")

<!NAME_CONTAINS_ILLEGAL_CHARS!>@JsName("  ___  ")
private fun bar(): String<!> = TODO("")

@JsName("validName")
private fun ` .private with @JsName `(): String = TODO("")

private class <!NAME_CONTAINS_ILLEGAL_CHARS!>` .private class `<!> {
    <!NAME_CONTAINS_ILLEGAL_CHARS!>val ` .field. `<!> = ""
}

val x: Int
    <!NAME_CONTAINS_ILLEGAL_CHARS!>@JsName(".")
    get()<!> = TODO("")

fun box(x: dynamic) {
    x.<!NAME_CONTAINS_ILLEGAL_CHARS!>`foo-bar`<!>()
    x.<!NAME_CONTAINS_ILLEGAL_CHARS!>`ba-z`<!>
}
