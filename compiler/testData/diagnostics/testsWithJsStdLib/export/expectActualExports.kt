// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_BACKEND_K1: JS_IR
// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +AllowExpectDeclarationsInJsExport +MultiPlatformProjects

// MODULE: commonMain
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

// Functions
@kotlin.js.JsExport expect fun foo(): Int

@kotlin.js.JsExport expect fun bar(): Int

// Classes
@kotlin.js.JsExport expect class Foo
@kotlin.js.JsExport expect class Bar
@kotlin.js.JsExport expect class Baz {
    <!WRONG_EXPORTED_DECLARATION("suspend function"), WRONG_EXPORTED_DECLARATION{METADATA}("suspend function")!>suspend fun foo(): Int<!>
}
@kotlin.js.JsExport expect class Nested {
    interface A
}
@kotlin.js.JsExport expect class Test1 {
    fun test()
}
@kotlin.js.JsExport expect class Test2 {
    fun test()
}
@kotlin.js.JsExport expect class Test3 {
    fun test()
}

// MODULE: jsMain()()(commonMain)
// TARGET_PLATFORM: JS
// FILE: js.kt
package sample

// Functions
<!NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED!>actual fun foo()<!> = 42

@kotlin.js.JsExport actual fun bar() = 42

// Classes
actual class <!NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED!>Foo<!>

@kotlin.js.JsExport actual class Bar {
    <!WRONG_EXPORTED_DECLARATION("suspend function")!>suspend fun foo()<!> = 42
}

@kotlin.js.JsExport actual class Nested {
    @kotlin.js.JsExport.Ignore actual interface <!NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED!>A<!>
}

@kotlin.js.JsExport class ExportedOne { fun test() {} }
class NotExportedOne { fun test() {} }
@kotlin.js.JsExport interface ExportedInterface { fun test() }

<!NOT_EXPORTED_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED!>actual typealias Test1 = NotExportedOne<!>
actual typealias Test2 = ExportedOne
actual typealias <!EXPECT_ACTUAL_INCOMPATIBLE_CLASS_KIND!>Test3<!> = ExportedInterface
