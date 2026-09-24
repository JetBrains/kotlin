// RUN_PIPELINE_TILL: FRONTEND
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

// FILE: nonModuleFile.kt
@file:JsNonModule
package nonModuleFile

<!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_ERROR!>external var nonModuleVar: Int<!>

// FILE: bothFile.kt
@file:JsModule("bothFile")
@file:JsNonModule
package bothFile

<!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_ERROR!>external var bothVar: Int<!>

// FILE: qualifierFile.kt
@file:JsQualifier("qualifierFile")
package qualifierFile

external var qualifierVar: Int

// FILE: plainFile.kt
package plainFile

external var plainVar: Int
