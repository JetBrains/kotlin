// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// LANGUAGE: +ProhibitVarInJsModuleFile
// ISSUE: KT-88343

// FILE: moduleFile.kt
@file:JsModule("moduleFile")
package moduleFile

<!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_ERROR!>external var moduleVar: Int<!>

external val moduleVal: Int

external object Obj {
    var memberVar: Int
}

external class Cls {
    var memberVar: Int
}

<!JS_MODULE_PROHIBITED_ON_VAR, NESTED_JS_MODULE_PROHIBITED!>@JsModule("annotated")
external var annotatedVar: Int<!>

// FILE: qualifierFile.kt
@file:JsQualifier("qualifierFile")
package qualifierFile

external var qualifierVar: Int

// FILE: plainFile.kt
package plainFile

external var plainVar: Int
