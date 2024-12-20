// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +MultiPlatformProjects
// MODULE: commonjs
// FILE: commonjs.kt

expect interface <!NO_ACTUAL_FOR_EXPECT!>ExternalInterface<!>

external class <!EXTERNAL_TYPE_EXTENDS_NON_EXTERNAL_TYPE!>ExternalClass<!>: ExternalInterface

// MODULE: js()()(commonjs)
// FILE: js.kt

actual external interface ExternalInterface
