// RUN_PIPELINE_TILL: FRONTEND
// OPT_IN: kotlin.js.ExperimentalJsExport
// MODULE: commonwasm
// FILE: commonwasm.kt

package sample

@JsExport expect fun foo(): Int

@JsExport expect fun bar(): Int

@JsExport expect fun baz(): Int

@JsExport expect fun ignored(): Int

expect fun notExported(): Int

// MODULE: wasm()()(commonwasm)
// FILE: wasm.kt

package sample

<!NOT_EXPORTED_OR_EXTERNAL_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED("mark the actual declaration with '@JsExport' as well")!>actual fun foo()<!> = 42

@JsExport actual fun bar() = 42

<!NOT_EXPORTED_OR_EXTERNAL_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED("mark the actual declaration with '@JsExport' as well")!>actual external fun baz(): Int<!>

<!NOT_EXPORTED_OR_EXTERNAL_ACTUAL_DECLARATION_WHILE_EXPECT_IS_EXPORTED("remove the '@JsExport.Ignore' annotation")!>@JsExport.Ignore actual fun ignored()<!> = 42

actual fun notExported() = 42
