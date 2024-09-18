// FIR_IDENTICAL
// IGNORE_BACKEND_K1: JS

const val VALUE = 123

fun testJavaScriptError() {
    js(<!JSCODE_ERROR!>"var = $VALUE;"<!>)
}
