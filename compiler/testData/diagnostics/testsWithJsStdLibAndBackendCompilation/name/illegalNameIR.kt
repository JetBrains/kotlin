// FIR_IDENTICAL
// IGNORE_BACKEND: JS_IR
// TODO: Move here all the contents of compiler/testData/diagnostics/testsWithJsStdLib/name/illegalNameIR.kt, after KT-67057 is fixed
// LANGUAGE: +JsAllowInvalidCharsIdentifiersEscaping

private fun ` .private `(): String = TODO("")

fun ` .public `(): String = TODO("")

@JsName("validName")
private fun ` .private with @JsName `(): String = TODO("")

private class ` .private class ` {
    val ` .field. ` = ""
}
