// RUN_PIPELINE_TILL: CODEGEN
// LANGUAGE: +MultiPlatformProjects
// MODULE: commonjs
// FILE: commonjs.kt

expect interface ExternalInterface

external class ExternalClass: ExternalInterface

// MODULE: js()()(commonjs)
// FILE: js.kt

actual external interface <!JS_ACTUAL_EXTERNAL_INTERFACE_WHILE_EXPECT_WITHOUT_JS_NO_RUNTIME!>ExternalInterface<!>
