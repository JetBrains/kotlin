// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalWasmJsInterop
// LATEST_LV_DIFFERENCE
// ISSUE: KT-88343

// FILE: moduleFile.kt
@file:JsModule("moduleFile")
package moduleFile

external <!JS_MODULE_PROHIBITED_ON_VAR_IN_MODULE_FILE_WARNING!>var<!> moduleVar: Int

external val moduleVal: Int

// A qualified declaration is referenced through its qualifier, so writing to it does reach the module.
@JsQualifier("qualified")
external var qualifiedVar: Int

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

// FILE: moduleWithQualifierFile.kt
@file:JsModule("moduleWithQualifierFile")
@file:JsQualifier("a.b")
package moduleWithQualifierFile

external var qualifiedFileVar: Int

// FILE: qualifierFile.kt
@file:JsQualifier("qualifierFile")
package qualifierFile

external var qualifierVar: Int

// FILE: plainFile.kt
package plainFile

external var plainVar: Int
