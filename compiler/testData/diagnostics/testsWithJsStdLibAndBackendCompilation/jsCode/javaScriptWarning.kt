// RUN_PIPELINE_TILL: CODEGEN

const val VALUE = 888

fun testJavaScriptWarning() {
    js(<!JSCODE_WARNING!>"var a = 0$VALUE;"<!>)
}
