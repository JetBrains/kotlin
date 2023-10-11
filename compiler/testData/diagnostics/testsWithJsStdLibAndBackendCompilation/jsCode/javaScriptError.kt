// FIR_IDENTICAL
// ERROR_POLICY: SEMANTIC

const val VALUE = 123

fun testJavaScriptError() {
    js(<!JSCODE_ERROR!>"var = $VALUE;"<!>)
}
