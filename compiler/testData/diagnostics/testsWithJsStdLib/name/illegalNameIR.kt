// RUN_PIPELINE_TILL: FIR2IR
// IGNORE_NON_REVERSED_RESOLVE: KT-62937
// IGNORE_REVERSED_RESOLVE: KT-62937
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

private fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .private `<!>(): String = TODO("")

fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .public `<!>(): String = TODO("")

@JsName("  __  ")
fun foo(): String = TODO("")

@JsName("  ___  ")
private fun bar(): String = TODO("")

@JsName("validName")
private fun <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .private with @JsName `<!>(): String = TODO("")

private class <!DOT_IN_TOP_LEVEL_DECLARATION_NAME!>` .private class `<!> {
    val ` .field. ` = ""
}

val x: Int
    @JsName(".")
    get() = TODO("")

fun box(x: dynamic) {
    x.`foo-bar`()
    x.`ba-z`
}
