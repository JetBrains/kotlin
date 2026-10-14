// RUN_PIPELINE_TILL: FRONTEND
// LATEST_LV_DIFFERENCE
// ISSUE: KT-88343

// FILE: moduleFile.kt
@file:JsModule("moduleFile")
package moduleFile

external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> moduleVar: Int

external val moduleVal: Int

// Unlike on Wasm, a qualifier does not help: the imported value is copied into a local variable.
@JsQualifier("qualified")
external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> qualifiedVar: Int

external object Obj {
    var memberVar: Int
}

external class Cls {
    var memberVar: Int
}

<!JS_MODULE_PROHIBITED_ON_VAR, NESTED_JS_MODULE_PROHIBITED!>@JsModule("annotated")
external var annotatedVar: Int<!>

@Suppress("JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING")
external var suppressedVar: Int

// FILE: nonModuleFile.kt
@file:JsNonModule
package nonModuleFile

external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> nonModuleVar: Int

// FILE: bothFile.kt
@file:JsModule("bothFile")
@file:JsNonModule
package bothFile

external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> bothVar: Int

// FILE: moduleWithQualifierFile.kt
@file:JsModule("moduleWithQualifierFile")
@file:JsQualifier("a.b")
package moduleWithQualifierFile

external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> qualifiedFileVar: Int

// FILE: qualifierFile.kt
@file:JsQualifier("qualifierFile")
package qualifierFile

external var qualifierVar: Int

// FILE: plainFile.kt
package plainFile

external var plainVar: Int
