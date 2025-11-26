// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// RUN_PIPELINE_TILL: FRONTEND
// LAST_
// IGNORE_BACKEND_K1: JS_IR

const val VALUE = 123

fun testJavaScriptError() {
    js(<!JSCODE_ERROR!>"var = $VALUE;"<!>)
}
