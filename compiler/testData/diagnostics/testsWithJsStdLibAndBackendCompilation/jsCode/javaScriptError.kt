// LANGUAGE: -IrIntraModuleInlinerBeforeKlibSerialization -IrCrossModuleInlinerBeforeKlibSerialization
// RUN_PIPELINE_TILL: BACKEND

const val VALUE = 123

fun testJavaScriptError() {
    js(<!JSCODE_ERROR!>"var = $VALUE;"<!>)
}
